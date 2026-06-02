package com.example.backend.repository;

import com.example.backend.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByAssureIdOrderByCreatedAtDesc(String assureId);
    List<Ticket> findByStatut(Ticket.StatutTicket statut);
    List<Ticket> findByAssigneA(String assigneA);
}
