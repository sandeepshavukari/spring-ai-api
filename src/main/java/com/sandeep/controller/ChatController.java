package com.sandeep.controller;

import com.sandeep.dto.request.ChatRequest;
import com.sandeep.dto.response.ApiResponse;
import com.sandeep.dto.response.ChatSessionResponse;
import com.sandeep.dto.response.MessageResponse;
import com.sandeep.model.Role;
import com.sandeep.security.CustomUserDetails;
import com.sandeep.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(chatService.createSession(userId(auth))));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessions(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getUserSessions(userId(auth))));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable Long sessionId, Authentication auth) {
        chatService.deleteSession(sessionId, userId(auth));
        return ResponseEntity.ok(ApiResponse.ok("Session deleted"));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable Long sessionId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                chatService.getSessionMessages(sessionId, userId(auth))
        ));
    }

    @PostMapping("/message")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody ChatRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Set<Role> roles = userDetails.getUser().getRoles();
        return ResponseEntity.ok(ApiResponse.success(
                chatService.chat(userDetails.getUserId(), roles, request)
        ));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(
            @Valid @RequestBody ChatRequest request, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Set<Role> roles = userDetails.getUser().getRoles();
        return chatService.streamChat(userDetails.getUserId(), roles, request);
    }

    private Long userId(Authentication auth) {
        return ((CustomUserDetails) auth.getPrincipal()).getUserId();
    }
}
