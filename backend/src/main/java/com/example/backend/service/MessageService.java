package com.example.backend.service;

import com.example.backend.dto.ConversationResponse;
import com.example.backend.dto.MessageRequest;
import com.example.backend.dto.MessageResponse;
import com.example.backend.model.Conversation;
import com.example.backend.model.Message;
import com.example.backend.model.User;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageResponse envoyerMessage(MessageRequest request) {
        String email = getCurrentUserEmail();
        User expediteur = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Conversation conversation;

        if (request.getConversationId() != null && !request.getConversationId().isBlank()) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));
        } else {
            if (request.getDestinataireId() == null || request.getDestinataireId().isBlank()) {
                throw new RuntimeException("Destinataire requis pour une nouvelle conversation");
            }

            User destinataire = userRepository.findById(request.getDestinataireId())
                    .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

            String p1 = expediteur.getRole().name().equals("GESTIONNAIRE") ? expediteur.getId() : destinataire.getId();
            String p2 = expediteur.getRole().name().equals("GESTIONNAIRE") ? destinataire.getId() : expediteur.getId();

            Optional<Conversation> existing = conversationRepository.findByParticipant1IdAndParticipant2Id(p1, p2);
            conversation = existing.orElseGet(() -> {
                Conversation c = new Conversation();
                c.setParticipant1Id(p1);
                c.setParticipant2Id(p2);
                c.setMessagesNonLusParticipant1(0);
                c.setMessagesNonLusParticipant2(0);
                c.setCreatedAt(LocalDateTime.now());
                c.setUpdatedAt(LocalDateTime.now());
                return conversationRepository.save(c);
            });
        }

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setExpediteurId(expediteur.getId());
        message.setContenu(request.getContenu());
        message.setLu(false);
        message.setCreatedAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        conversation.setDernierMessage(request.getContenu());
        conversation.setDernierMessageDate(LocalDateTime.now());

        if (expediteur.getId().equals(conversation.getParticipant1Id())) {
            conversation.setMessagesNonLusParticipant2(conversation.getMessagesNonLusParticipant2() + 1);
        } else {
            conversation.setMessagesNonLusParticipant1(conversation.getMessagesNonLusParticipant1() + 1);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return mapToMessageResponse(saved, expediteur);
    }

    public List<ConversationResponse> getMesConversations() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Conversation> conversations = conversationRepository
                .findByParticipant1IdOrParticipant2IdOrderByDernierMessageDateDesc(user.getId(), user.getId());

        return conversations.stream()
                .map(c -> mapToConversationResponse(c, user.getId()))
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getMessagesConversation(String conversationId) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        if (!conversation.getParticipant1Id().equals(user.getId())
                && !conversation.getParticipant2Id().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé à cette conversation");
        }

        marquerCommeLues(conversationId, user.getId());

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream()
                .map(m -> mapToMessageResponse(m))
                .collect(Collectors.toList());
    }

    private void marquerCommeLues(String conversationId, String userId) {
        List<Message> nonLus = messageRepository
                .findByConversationIdAndLuFalseAndExpediteurIdNot(conversationId, userId);
        for (Message m : nonLus) {
            m.setLu(true);
        }
        messageRepository.saveAll(nonLus);

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation != null) {
            if (conversation.getParticipant1Id().equals(userId)) {
                conversation.setMessagesNonLusParticipant1(0);
            } else {
                conversation.setMessagesNonLusParticipant2(0);
            }
            conversationRepository.save(conversation);
        }
    }

    private MessageResponse mapToMessageResponse(Message message) {
        User expediteur = userRepository.findById(message.getExpediteurId()).orElse(null);
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .expediteurId(message.getExpediteurId())
                .expediteurNom(expediteur != null ? expediteur.getFullName() : "Inconnu")
                .expediteurRole(expediteur != null ? expediteur.getRole().name() : null)
                .contenu(message.getContenu())
                .lu(message.isLu())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message message, User expediteur) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .expediteurId(message.getExpediteurId())
                .expediteurNom(expediteur.getFullName())
                .expediteurRole(expediteur.getRole().name())
                .contenu(message.getContenu())
                .lu(message.isLu())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ConversationResponse mapToConversationResponse(Conversation c, String currentUserId) {
        User p1 = userRepository.findById(c.getParticipant1Id()).orElse(null);
        User p2 = userRepository.findById(c.getParticipant2Id()).orElse(null);

        int nonLus;
        if (c.getParticipant1Id().equals(currentUserId)) {
            nonLus = c.getMessagesNonLusParticipant1();
        } else {
            nonLus = c.getMessagesNonLusParticipant2();
        }

        return ConversationResponse.builder()
                .id(c.getId())
                .participant1Id(c.getParticipant1Id())
                .participant1Nom(p1 != null ? p1.getFullName() : "Inconnu")
                .participant1Role(p1 != null ? p1.getRole().name() : null)
                .participant2Id(c.getParticipant2Id())
                .participant2Nom(p2 != null ? p2.getFullName() : "Inconnu")
                .participant2Role(p2 != null ? p2.getRole().name() : null)
                .claimId(c.getClaimId())
                .dernierMessage(c.getDernierMessage())
                .dernierMessageDate(c.getDernierMessageDate())
                .messagesNonLus(nonLus)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return authentication.getName();
    }
}
