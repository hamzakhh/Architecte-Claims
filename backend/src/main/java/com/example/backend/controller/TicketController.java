package com.example.backend.controller;

import com.example.backend.dto.TicketMessageRequest;
import com.example.backend.dto.TicketRequest;
import com.example.backend.dto.TicketResponse;
import com.example.backend.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasRole('ASSURE')")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.ok(ticketService.createTicket(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('ASSURE')")
    public ResponseEntity<List<TicketResponse>> getMyTickets() {
        return ResponseEntity.ok(ticketService.getMyTickets());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponse> addMessage(@PathVariable String id, @Valid @RequestBody TicketMessageRequest request) {
        return ResponseEntity.ok(ticketService.addMessage(id, request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(@PathVariable String id, @RequestParam String gestionnaireId) {
        return ResponseEntity.ok(ticketService.assignTicket(id, gestionnaireId));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.resoudreTicket(id));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMIN')")
    public ResponseEntity<TicketResponse> closeTicket(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.fermerTicket(id));
    }
}
