package com.example.backend.service;

import com.example.backend.config.OllamaConfig;
import com.example.backend.dto.AnalyseIARequest;
import com.example.backend.dto.AnalyseIAResponse;
import com.example.backend.model.AnalyseIA;
import com.example.backend.model.Claim;
import com.example.backend.repository.AnalyseIARepository;
import com.example.backend.repository.ClaimRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service d'analyse IA des sinistres.
 * Utilise l'API Ollama (LLM local) pour analyser les sinistres et produire
 * des pré-évaluations automatiques, des scores de risque et des recommandations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyseIAService {

    private final AnalyseIARepository analyseIARepository;
    private final ClaimRepository claimRepository;
    private final OllamaConfig ollamaConfig;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = WebClient.builder()
                    .baseUrl(ollamaConfig.getApiUrl())
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }
        return webClient;
    }

    /**
     * Analyse un sinistre via Ollama.
     * Si l'IA est désactivée ou en erreur, utilise l'analyse simulée.
     */
    public AnalyseIAResponse analyserSinistre(AnalyseIARequest request) {
        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + request.getClaimId()));

        AnalyseIA.TypeAnalyse typeAnalyse = request.getTypeAnalyse() != null
                ? AnalyseIA.TypeAnalyse.valueOf(request.getTypeAnalyse())
                : AnalyseIA.TypeAnalyse.INITIALE;

        // Vérifier si une analyse existe déjà pour ce claim
        AnalyseIA existingAnalyse = analyseIARepository.findByClaimId(request.getClaimId()).orElse(null);
        if (existingAnalyse != null && typeAnalyse == AnalyseIA.TypeAnalyse.INITIALE) {
            log.info("Analyse IA existante trouvée pour le sinistre {}", request.getClaimId());
            return mapToResponse(existingAnalyse, claim);
        }

        AnalyseIA analyse = existingAnalyse != null ? existingAnalyse : new AnalyseIA();
        analyse.setClaimId(request.getClaimId());
        analyse.setTypeAnalyse(typeAnalyse);
        analyse.setStatut(AnalyseIA.StatutAnalyse.EN_COURS);
        analyse.setCreatedAt(existingAnalyse != null ? existingAnalyse.getCreatedAt() : LocalDateTime.now());
        analyse.setUpdatedAt(LocalDateTime.now());
        analyse = analyseIARepository.save(analyse);

        try {
            if (ollamaConfig.isEnabled()) {
                analyse = callOllama(claim, analyse, typeAnalyse);
            } else {
                analyse = analyseSimulee(claim, analyse);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'appel Ollama, fallback vers analyse simulée: {}", e.getMessage());
            analyse = analyseSimulee(claim, analyse);
        }

        analyse = analyseIARepository.save(analyse);
        return mapToResponse(analyse, claim);
    }

    /**
     * Appelle l'API Ollama pour analyser le sinistre.
     */
    private AnalyseIA callOllama(Claim claim, AnalyseIA analyse, AnalyseIA.TypeAnalyse typeAnalyse) {
        log.info("Appel Ollama ({}) pour le sinistre {} (type: {})", ollamaConfig.getModel(), claim.getId(), typeAnalyse);

        String systemPrompt = buildSystemPrompt(typeAnalyse);
        String userPrompt = buildUserPrompt(claim);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", ollamaConfig.getTemperature());
        options.put("num_predict", ollamaConfig.getMaxTokens());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("format", "json");
        requestBody.put("options", options);

        try {
            String responseBody = getWebClient()
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseOllamaResponse(responseBody, analyse);
        } catch (Exception e) {
            log.error("Erreur API Ollama: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'appel à l'API Ollama: " + e.getMessage());
        }
    }

    /**
     * Construit le prompt système pour Ollama.
     */
    private String buildSystemPrompt(AnalyseIA.TypeAnalyse typeAnalyse) {
        String basePrompt = """
                Tu es un expert en assurance sinistres. Tu analyses les déclarations de sinistres pour une compagnie d'assurance française.
                
                Pour chaque sinistre, tu dois fournir une analyse JSON avec les champs suivants :
                - scoreComplexite: entier 0-100 (0=très simple, 100=très complexe)
                - scoreRisque: entier 0-100 (0=aucun risque, 100=risque très élevé de fraude ou problème)
                - scoreConfiance: entier 0-100 (0=aucune confiance, 100=confiance totale dans l'analyse)
                - montantEstime: nombre (estimation des dommages en euros)
                - severite: "FAIBLE", "MODEREE", "ELEVEE" ou "CRITIQUE"
                - categorieDetectee: catégorie du sinistre (accident, incendie, vol, degat_eaux, catastrophe, autre)
                - motsCles: liste de mots-clés extraits de la description
                - necessiteExpertHumain: booléen (true si un expert humain est requis)
                - recommandation: texte court de la recommandation (ex: "Validation automatique possible" ou "Expertise requise")
                - justification: justification détaillée de la recommandation
                - resumeAnalyse: résumé de l'analyse en 2-3 phrases
                - pointsAttention: points d'attention identifiés (séparés par des retours à la ligne)
                - elementsFraude: éléments suspects ou vide si aucun (séparés par des retours à la ligne)
                - recommandationsAction: actions recommandées pour le gestionnaire (séparées par des retours à la ligne)
                
                Réponds UNIQUEMENT avec du JSON valide, sans texte avant ou après.
                """;

        if (typeAnalyse == AnalyseIA.TypeAnalyse.VERIFICATION_FRAUDE) {
            basePrompt += "\n\nATTENTION: Cette analyse est une vérification anti-fraude ciblée. Sois particulièrement vigilant sur les incohérences, les signes de fraude, les montants exagérés, les descriptions vagues ou contradictoires.";
        } else if (typeAnalyse == AnalyseIA.TypeAnalyse.APPROFONDIE) {
            basePrompt += "\n\nATTENTION: Cette analyse est approfondie. Fournis une analyse plus détaillée avec des recommandations précises.";
        }

        return basePrompt;
    }

    /**
     * Construit le prompt utilisateur avec les données du sinistre.
     */
    private String buildUserPrompt(Claim claim) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse le sinistre suivant :\n\n");
        sb.append("Référence: ").append(claim.getReference()).append("\n");
        sb.append("Type: ").append(claim.getType()).append("\n");
        sb.append("Catégorie: ").append(claim.getCategorie()).append("\n");
        sb.append("Description: ").append(claim.getDescription()).append("\n");
        sb.append("Date du sinistre: ").append(claim.getDateSinistre()).append("\n");
        sb.append("Heure: ").append(claim.getHeureSinistre()).append("\n");
        sb.append("Lieu: ").append(claim.getLieu()).append("\n");
        if (claim.getEstimation() != null) {
            sb.append("Estimation déclarée: ").append(claim.getEstimation()).append("\n");
        }
        if (claim.getPiecesJointes() != null && !claim.getPiecesJointes().isEmpty()) {
            sb.append("Nombre de pièces jointes: ").append(claim.getPiecesJointes().size()).append("\n");
            sb.append("Fichiers: ").append(String.join(", ", claim.getPiecesJointes())).append("\n");
        }
        if (claim.getNotesLieu() != null) {
            sb.append("Notes sur le lieu: ").append(claim.getNotesLieu()).append("\n");
        }
        sb.append("\nFournis l'analyse au format JSON.");
        return sb.toString();
    }

    /**
     * Parse la réponse de l'API Ollama.
     */
    private AnalyseIA parseOllamaResponse(String responseBody, AnalyseIA analyse) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Métadonnées Ollama
            int evalCount = root.has("eval_count") ? root.get("eval_count").asInt() : 0;
            int promptEvalCount = root.has("prompt_eval_count") ? root.get("prompt_eval_count").asInt() : 0;
            analyse.setTokensUtilises(evalCount + promptEvalCount);
            analyse.setModeleIA(ollamaConfig.getModel());
            analyse.setCoutEstime(0.0); // Ollama est local, coût = 0

            // Extraire le contenu de la réponse Ollama
            String content = root.get("message").get("content").asText();

            // Nettoyer le contenu (parfois le modèle ajoute des balises markdown)
            String cleanContent = content.trim();
            if (cleanContent.startsWith("```json")) {
                cleanContent = cleanContent.substring(7);
            } else if (cleanContent.startsWith("```")) {
                cleanContent = cleanContent.substring(3);
            }
            if (cleanContent.endsWith("```")) {
                cleanContent = cleanContent.substring(0, cleanContent.length() - 3);
            }
            cleanContent = cleanContent.trim();

            JsonNode analysis = objectMapper.readTree(cleanContent);

            analyse.setScoreComplexite(analysis.get("scoreComplexite").asInt());
            analyse.setScoreRisque(analysis.get("scoreRisque").asInt());
            analyse.setScoreConfiance(analysis.get("scoreConfiance").asInt());
            analyse.setMontantEstime(analysis.has("montantEstime") ? analysis.get("montantEstime").asDouble() : null);
            analyse.setDevise("TND");
            analyse.setSeverite(AnalyseIA.Severite.valueOf(analysis.get("severite").asText()));
            analyse.setCategorieDetectee(analysis.get("categorieDetectee").asText());

            if (analysis.has("motsCles") && analysis.get("motsCles").isArray()) {
                List<String> motsCles = new ArrayList<>();
                for (JsonNode mot : analysis.get("motsCles")) {
                    motsCles.add(mot.asText());
                }
                analyse.setMotsCles(motsCles);
            }

            analyse.setNecessiteExpertHumain(analysis.get("necessiteExpertHumain").asBoolean());
            analyse.setRecommandation(analysis.get("recommandation").asText());
            analyse.setJustification(analysis.get("justification").asText());
            analyse.setResumeAnalyse(analysis.get("resumeAnalyse").asText());
            analyse.setPointsAttention(analysis.has("pointsAttention") ? analysis.get("pointsAttention").asText() : null);
            analyse.setElementsFraude(analysis.has("elementsFraude") ? analysis.get("elementsFraude").asText() : null);
            analyse.setRecommandationsAction(analysis.has("recommandationsAction") ? analysis.get("recommandationsAction").asText() : null);

            analyse.setStatut(AnalyseIA.StatutAnalyse.TERMINEE);
            analyse.setDateAnalyse(LocalDateTime.now());
            analyse.setUpdatedAt(LocalDateTime.now());

            log.info("Analyse Ollama terminée pour le sinistre {} - Sévérité: {}, Expert requis: {}",
                    analyse.getClaimId(), analyse.getSeverite(), analyse.isNecessiteExpertHumain());

        } catch (Exception e) {
            log.error("Erreur lors du parsing de la réponse Ollama: {}", e.getMessage());
            analyse.setStatut(AnalyseIA.StatutAnalyse.ERREUR);
            analyse.setUpdatedAt(LocalDateTime.now());
        }

        return analyse;
    }

    /**
     * Analyse simulée (fallback quand Ollama n'est pas disponible).
     * Utilise des heuristiques pour produire une analyse cohérente.
     */
    private AnalyseIA analyseSimulee(Claim claim, AnalyseIA analyse) {
        log.info("Analyse simulée pour le sinistre {}", claim.getId());

        String description = claim.getDescription() != null ? claim.getDescription().toLowerCase() : "";
        String type = claim.getType() != null ? claim.getType() : "autre";
        String estimation = claim.getEstimation();

        // Score de complexité basé sur le type et la description
        int complexite = switch (type) {
            case "auto" -> 35;
            case "fire" -> 60;
            case "water" -> 45;
            case "theft" -> 50;
            case "natural" -> 75;
            default -> 40;
        };

        // Ajuster selon la longueur de la description
        if (description.length() > 500) complexite = Math.min(100, complexite + 15);
        if (description.contains("grave") || description.contains("important") || description.contains("critique"))
            complexite = Math.min(100, complexite + 20);

        // Score de risque
        int risque = 20;
        if (description.contains("suspect") || description.contains("fraude")) risque += 40;
        if (estimation != null) {
            try {
                double montant = Double.parseDouble(estimation.replaceAll("[^0-9.]", ""));
                if (montant > 50000) risque += 30;
                else if (montant > 10000) risque += 15;
            } catch (NumberFormatException ignored) {}
        }
        if (claim.getPiecesJointes() == null || claim.getPiecesJointes().isEmpty()) risque += 10;
        risque = Math.min(100, risque);

        // Score de confiance (inverse du risque)
        int confiance = Math.max(10, 100 - risque - (complexite / 4));

        // Sévérité
        AnalyseIA.Severite severite;
        boolean necessiteExpert;
        if (complexite >= 70 || risque >= 60) {
            severite = AnalyseIA.Severite.CRITIQUE;
            necessiteExpert = true;
        } else if (complexite >= 50 || risque >= 40) {
            severite = AnalyseIA.Severite.ELEVEE;
            necessiteExpert = true;
        } else if (complexite >= 30 || risque >= 25) {
            severite = AnalyseIA.Severite.MODEREE;
            necessiteExpert = false;
        } else {
            severite = AnalyseIA.Severite.FAIBLE;
            necessiteExpert = false;
        }

        // Montant estimé
        Double montantEstime = null;
        if (estimation != null) {
            try {
                montantEstime = Double.parseDouble(estimation.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ignored) {}
        }

        // Catégorie détectée
        String categorieDetectee = switch (type) {
            case "auto" -> "accident";
            case "fire" -> "incendie";
            case "theft" -> "vol";
            case "water" -> "degat_eaux";
            case "natural" -> "catastrophe";
            default -> "autre";
        };

        List<String> motsCles = new ArrayList<>();
        if (description.contains("accident")) motsCles.add("accident");
        if (description.contains("incendie") || description.contains("feu")) motsCles.add("incendie");
        if (description.contains("vol") || description.contains("cambriolage")) motsCles.add("vol");
        if (description.contains("inondation") || description.contains("eau")) motsCles.add("degat_eaux");
        if (description.contains("véhicule") || description.contains("voiture")) motsCles.add("automobile");
        if (description.contains("maison") || description.contains("habitation")) motsCles.add("habitation");
        if (motsCles.isEmpty()) motsCles.add(type);

        analyse.setScoreComplexite(complexite);
        analyse.setScoreRisque(risque);
        analyse.setScoreConfiance(confiance);
        analyse.setMontantEstime(montantEstime);
        analyse.setDevise("TND");
        analyse.setSeverite(severite);
        analyse.setCategorieDetectee(categorieDetectee);
        analyse.setMotsCles(motsCles);
        analyse.setNecessiteExpertHumain(necessiteExpert);
        analyse.setRecommandation(necessiteExpert
                ? "Expertise humaine recommandée - sinistre complexe ou à risque"
                : "Validation automatique possible - sinistre simple et fiable");
        analyse.setJustification(String.format("Complexité: %d/100, Risque: %d/100, Confiance: %d/100. %s",
                complexite, risque, confiance,
                necessiteExpert ? "Les scores justifient l'intervention d'un expert." : "Les scores permettent une validation automatique."));
        analyse.setResumeAnalyse(String.format("Sinistre de type %s analysé. Sévérité: %s. %s",
                categorieDetectee, severite.name(),
                necessiteExpert ? "Expertise humaine requise." : "Traitement automatique possible."));
        analyse.setPointsAttention(risque > 30 ? "Risque de fraude détecté. Vérifier les documents fournis." : "Aucun point d'attention majeur.");
        analyse.setElementsFraude(risque > 40 ? "Incohérences potentielles dans la déclaration. Vérification recommandée." : "Aucun élément de fraude détecté.");
        analyse.setRecommandationsAction(necessiteExpert
                ? "1. Assigner un expert spécialisé\n2. Vérifier les pièces jointes\n3. Comparer avec les sinistres similaires"
                : "1. Valider automatiquement\n2. Proposer une indemnisation\n3. Notifier l'assuré");
        analyse.setModeleIA("simulation");
        analyse.setTokensUtilises(0);
        analyse.setCoutEstime(0.0);
        analyse.setStatut(AnalyseIA.StatutAnalyse.TERMINEE);
        analyse.setDateAnalyse(LocalDateTime.now());
        analyse.setUpdatedAt(LocalDateTime.now());

        return analyse;
    }

    /**
     * Récupère l'analyse IA d'un sinistre.
     */
    public AnalyseIAResponse getAnalyseByClaimId(String claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Sinistre non trouvé: " + claimId));
        AnalyseIA analyse = analyseIARepository.findByClaimId(claimId)
                .orElseThrow(() -> new RuntimeException("Aucune analyse IA trouvée pour le sinistre: " + claimId));
        return mapToResponse(analyse, claim);
    }

    /**
     * Récupère toutes les analyses nécessitant un expert humain.
     */
    public List<AnalyseIAResponse> getAnalysesNecessitantExpert() {
        return analyseIARepository.findByNecessiteExpertHumain(true).stream()
                .map(a -> {
                    Claim claim = claimRepository.findById(a.getClaimId()).orElse(null);
                    return mapToResponse(a, claim);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Récupère toutes les analyses (admin).
     */
    public List<AnalyseIAResponse> getAllAnalyses() {
        return analyseIARepository.findAll().stream()
                .map(a -> {
                    Claim claim = claimRepository.findById(a.getClaimId()).orElse(null);
                    return mapToResponse(a, claim);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Récupère les statistiques des analyses IA.
     */
    public Map<String, Object> getAnalyseStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalAnalyses = analyseIARepository.count();
        long analysesTerminees = analyseIARepository.countByStatut(AnalyseIA.StatutAnalyse.TERMINEE);
        long analysesEnCours = analyseIARepository.countByStatut(AnalyseIA.StatutAnalyse.EN_COURS);
        long analysesErreur = analyseIARepository.countByStatut(AnalyseIA.StatutAnalyse.ERREUR);
        long necessitantExpert = analyseIARepository.countByNecessiteExpertHumain(true);
        long critiques = analyseIARepository.countBySeverite(AnalyseIA.Severite.CRITIQUE);

        stats.put("totalAnalyses", totalAnalyses);
        stats.put("analysesTerminees", analysesTerminees);
        stats.put("analysesEnCours", analysesEnCours);
        stats.put("analysesErreur", analysesErreur);
        stats.put("necessitantExpert", necessitantExpert);
        stats.put("critiques", critiques);
        stats.put("tauxExpertRequis", totalAnalyses > 0 ? (necessitantExpert * 100.0 / totalAnalyses) : 0);
        stats.put("ollamaEnabled", ollamaConfig.isEnabled());
        stats.put("modeleIA", ollamaConfig.getModel());

        return stats;
    }

    /**
     * Map une entité AnalyseIA vers un DTO AnalyseIAResponse.
     */
    private AnalyseIAResponse mapToResponse(AnalyseIA analyse, Claim claim) {
        return AnalyseIAResponse.builder()
                .id(analyse.getId())
                .claimId(analyse.getClaimId())
                .claimReference(claim != null ? claim.getReference() : null)
                .scoreComplexite(analyse.getScoreComplexite())
                .scoreRisque(analyse.getScoreRisque())
                .scoreConfiance(analyse.getScoreConfiance())
                .montantEstime(analyse.getMontantEstime())
                .devise(analyse.getDevise())
                .severite(analyse.getSeverite() != null ? analyse.getSeverite().name() : null)
                .categorieDetectee(analyse.getCategorieDetectee())
                .motsCles(analyse.getMotsCles())
                .necessiteExpertHumain(analyse.isNecessiteExpertHumain())
                .recommandation(analyse.getRecommandation())
                .justification(analyse.getJustification())
                .resumeAnalyse(analyse.getResumeAnalyse())
                .pointsAttention(analyse.getPointsAttention())
                .elementsFraude(analyse.getElementsFraude())
                .recommandationsAction(analyse.getRecommandationsAction())
                .typeAnalyse(analyse.getTypeAnalyse() != null ? analyse.getTypeAnalyse().name() : null)
                .statut(analyse.getStatut() != null ? analyse.getStatut().name() : null)
                .modeleIA(analyse.getModeleIA())
                .tokensUtilises(analyse.getTokensUtilises())
                .coutEstime(analyse.getCoutEstime())
                .dateAnalyse(analyse.getDateAnalyse())
                .createdAt(analyse.getCreatedAt())
                .build();
    }
}
