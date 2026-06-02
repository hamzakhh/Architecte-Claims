package com.example.backend.repository;

import com.example.backend.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    List<Conversation> findByParticipant1IdOrderByDernierMessageDateDesc(String participant1Id);

    List<Conversation> findByParticipant2IdOrderByDernierMessageDateDesc(String participant2Id);

    List<Conversation> findByParticipant1IdOrParticipant2IdOrderByDernierMessageDateDesc(String participant1Id, String participant2Id);

    Optional<Conversation> findByParticipant1IdAndParticipant2Id(String participant1Id, String participant2Id);

    List<Conversation> findByClaimIdOrderByDernierMessageDateDesc(String claimId);
}
