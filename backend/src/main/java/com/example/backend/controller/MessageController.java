package com.example.backend.controller;

import com.example.backend.dto.ConversationResponse;
import com.example.backend.dto.MessageRequest;
import com.example.backend.dto.MessageResponse;
import com.example.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<MessageResponse> envoyerMessage(@Valid @RequestBody MessageRequest request) {
        MessageResponse response = messageService.envoyerMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<List<ConversationResponse>> getMesConversations() {
        return ResponseEntity.ok(messageService.getMesConversations());
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("hasAnyRole('ASSURE', 'GESTIONNAIRE', 'EXPERT', 'ADMIN')")
    public ResponseEntity<List<MessageResponse>> getMessagesConversation(@PathVariable String conversationId) {
        return ResponseEntity.ok(messageService.getMessagesConversation(conversationId));
    }
}
