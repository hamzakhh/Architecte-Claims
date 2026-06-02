package com.example.backend.controller;

import com.example.backend.dto.ClaimResponse;
import com.example.backend.dto.ReimbursementResponse;
import com.example.backend.service.ClaimService;
import com.example.backend.service.ReimbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ClaimService claimService;
    private final ReimbursementService reimbursementService;

    @GetMapping("/claims/csv")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<byte[]> exportClaimsCsv(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        List<ClaimResponse> claims = claimService.getAllClaims();

        // Filtrer si nécessaire
        if (statut != null && !statut.isEmpty()) {
            claims = claims.stream().filter(c -> c.getStatut() != null && statut.equals(c.getStatut().name())).toList();
        }
        if (categorie != null && !categorie.isEmpty()) {
            claims = claims.stream().filter(c -> categorie.equals(c.getCategorie())).toList();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);

        // BOM pour Excel
        writer.write("\uFEFF");

        // En-tête
        writer.println("Référence;Catégorie;Type;Assuré;Description;Lieu;Date Sinistre;Statut;Estimation;Gestionnaire;Expert;Date Création;Dernière MAJ");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (ClaimResponse c : claims) {
            writer.println(String.join(";",
                    escape(c.getReference()),
                    escape(c.getCategorie()),
                    escape(c.getType()),
                    escape(c.getAssureNom()),
                    escape(c.getDescription()),
                    escape(c.getLieu()),
                    c.getDateSinistre() != null ? c.getDateSinistre() : "",
                    c.getStatut() != null ? c.getStatut().name() : "",
                    c.getEstimation() != null ? c.getEstimation() : "",
                    escape(c.getGestionnaireNom()),
                    escape(c.getExpertNom()),
                    c.getCreatedAt() != null ? c.getCreatedAt().format(fmt) : "",
                    c.getUpdatedAt() != null ? c.getUpdatedAt().format(fmt) : ""
            ));
        }

        writer.flush();

        String filename = "sinistres_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(baos.toByteArray());
    }

    @GetMapping("/reimbursements/csv")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<byte[]> exportReimbursementsCsv(
            @RequestParam(required = false) String statut
    ) {
        List<ReimbursementResponse> rembs = reimbursementService.getAllReimbursements();

        if (statut != null && !statut.isEmpty()) {
            rembs = rembs.stream().filter(r -> r.getStatut() != null && statut.equals(r.getStatut().name())).toList();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);

        writer.write("\uFEFF");
        writer.println("Référence;Sinistre;Assuré;Montant Proposé;Montant Final;Méthode Paiement;Statut;Motif Refus;Date Proposition;Date Validation;Date Paiement");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (ReimbursementResponse r : rembs) {
            writer.println(String.join(";",
                    escape(r.getReference()),
                    escape(r.getClaimReference()),
                    escape(r.getAssureNom()),
                    String.valueOf(r.getMontantPropose()),
                    String.valueOf(r.getMontantFinal()),
                    r.getMethodePaiement() != null ? r.getMethodePaiement().name() : "",
                    r.getStatut() != null ? r.getStatut().name() : "",
                    escape(r.getMotifRefus()),
                    r.getDateProposition() != null ? r.getDateProposition().format(fmt) : "",
                    r.getDateValidation() != null ? r.getDateValidation().format(fmt) : "",
                    r.getDatePaiement() != null ? r.getDatePaiement().format(fmt) : ""
            ));
        }

        writer.flush();

        String filename = "remboursements_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(baos.toByteArray());
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
