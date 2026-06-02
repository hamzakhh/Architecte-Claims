package com.example.backend.service;

import com.example.backend.dto.TicketMessageRequest;
import com.example.backend.dto.TicketRequest;
import com.example.backend.dto.TicketResponse;
import com.example.backend.model.Ticket;
import com.example.backend.model.User;
import com.example.backend.repository.TicketRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketResponse createTicket(TicketRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Ticket ticket = new Ticket();
        ticket.setAssureId(user.getId());
        ticket.setClaimId(request.getClaimId());
        ticket.setSujet(request.getSujet());
        ticket.setDescription(request.getDescription());
        ticket.setCategorie(request.getCategorie() != null ? request.getCategorie() : "general");
        ticket.setStatut(Ticket.StatutTicket.OUVERT);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticketRepository.save(ticket);
        log.info("Ticket créé: {} par {}", ticket.getId(), user.getEmail());
        return mapToResponse(ticket);
    }

    public List<TicketResponse> getMyTickets() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ticketRepository.findByAssureIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    public TicketResponse getTicketById(String id) {
        return mapToResponse(ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé")));
    }

    public TicketResponse addMessage(String ticketId, TicketMessageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Ticket.TicketMessage msg = new Ticket.TicketMessage();
        msg.setExpediteurId(user.getId());
        msg.setExpediteurNom(user.getFullName());
        msg.setContenu(request.getContenu());
        msg.setCreatedAt(LocalDateTime.now());

        ticket.getMessages().add(msg);
        ticket.setUpdatedAt(LocalDateTime.now());

        if (ticket.getStatut() == Ticket.StatutTicket.OUVERT || ticket.getStatut() == Ticket.StatutTicket.EN_ATTENTE) {
            ticket.setStatut(Ticket.StatutTicket.EN_COURS);
        }

        ticketRepository.save(ticket);
        return mapToResponse(ticket);
    }

    public TicketResponse assignTicket(String ticketId, String gestionnaireId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé"));
        ticket.setAssigneA(gestionnaireId);
        ticket.setStatut(Ticket.StatutTicket.EN_COURS);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return mapToResponse(ticket);
    }

    public TicketResponse resoudreTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé"));
        ticket.setStatut(Ticket.StatutTicket.RESOLU);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return mapToResponse(ticket);
    }

    public TicketResponse fermerTicket(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket non trouvé"));
        ticket.setStatut(Ticket.StatutTicket.FERME);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        return mapToResponse(ticket);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        String assureNom = userRepository.findById(ticket.getAssureId()).map(User::getFullName).orElse(null);
        String assigneNom = ticket.getAssigneA() != null ? userRepository.findById(ticket.getAssigneA()).map(User::getFullName).orElse(null) : null;

        List<TicketResponse.TicketMessageResponse> messages = ticket.getMessages() != null
                ? ticket.getMessages().stream().map(m -> TicketResponse.TicketMessageResponse.builder()
                        .expediteurId(m.getExpediteurId()).expediteurNom(m.getExpediteurNom())
                        .contenu(m.getContenu()).createdAt(m.getCreatedAt()).build())
                  .collect(Collectors.toList())
                : List.of();

        return TicketResponse.builder()
                .id(ticket.getId()).assureId(ticket.getAssureId()).assureNom(assureNom)
                .claimId(ticket.getClaimId()).sujet(ticket.getSujet())
                .description(ticket.getDescription()).categorie(ticket.getCategorie())
                .statut(ticket.getStatut()).assigneA(ticket.getAssigneA()).assigneNom(assigneNom)
                .messages(messages).createdAt(ticket.getCreatedAt()).updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
