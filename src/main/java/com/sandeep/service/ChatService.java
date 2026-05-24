package com.sandeep.service;

import com.sandeep.dto.request.ChatRequest;
import com.sandeep.dto.response.ChatSessionResponse;
import com.sandeep.dto.response.MessageResponse;
import com.sandeep.exception.ResourceNotFoundException;
import com.sandeep.exception.UnauthorizedException;
import com.sandeep.model.ChatSession;
import com.sandeep.model.FileDocument;
import com.sandeep.model.MessageRecord;
import com.sandeep.model.Role;
import com.sandeep.repository.ChatSessionRepository;
import com.sandeep.repository.FileDocumentRepository;
import com.sandeep.repository.MessageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final String SYSTEM_PROMPT =
            "You are a helpful AI assistant. Be concise, accurate, and friendly. " +
            "When document context is provided, use it to answer questions accurately. " +
            "When analyzing images, describe what you observe in detail.";

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepository;
    private final MessageRecordRepository messageRepository;
    private final FileDocumentRepository fileDocumentRepository;
    private final FileStorageService fileStorageService;
    private final RateLimitService rateLimitService;

    @Transactional
    public ChatSessionResponse createSession(Long userId) {
        ChatSession session = sessionRepository.save(ChatSession.builder()
                .userId(userId)
                .title("New Chat")
                .messageCount(0)
                .build());
        return toSessionResponse(session);
    }

    public List<ChatSessionResponse> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    public List<MessageResponse> getSessionMessages(Long sessionId, Long userId) {
        validateSessionOwnership(sessionId, userId);
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        validateSessionOwnership(sessionId, userId);
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    @Transactional
    public MessageResponse chat(Long userId, Set<Role> roles, ChatRequest request) {
        rateLimitService.checkAndIncrement(userId, roles);

        ChatSession session = resolveSession(userId, request);
        List<MessageRecord> history = messageRepository.findBySessionIdOrderByTimestampAsc(session.getId());

        saveMessage(session.getId(), "user", request.getMessage(), request.getFileId(), null);

        String aiReply;
        if (request.getFileId() != null) {
            aiReply = handleFileAttachment(request.getMessage(), request.getFileId(), history);
        } else {
            aiReply = callChat(request.getMessage(), history);
        }

        MessageRecord assistantMsg = saveMessage(session.getId(), "assistant", aiReply, null, null);
        updateSession(session, history.size() + 2);

        return toMessageResponse(assistantMsg);
    }

    public Flux<String> streamChat(Long userId, Set<Role> roles, ChatRequest request) {
        rateLimitService.checkAndIncrement(userId, roles);

        ChatSession session = resolveSession(userId, request);
        List<MessageRecord> history = messageRepository.findBySessionIdOrderByTimestampAsc(session.getId());

        saveMessage(session.getId(), "user", request.getMessage(), request.getFileId(), null);

        String systemContext = buildSystemContext(request.getFileId());
        List<Message> messages = buildHistory(history);

        final Long sessionId = session.getId();
        final int newCount = history.size() + 2;
        StringBuilder fullReply = new StringBuilder();

        return chatClient.prompt()
                .system(systemContext)
                .messages(messages)
                .user(request.getMessage())
                .stream()
                .content()
                .doOnNext(fullReply::append)
                .doOnComplete(() -> {
                    String reply = fullReply.toString();
                    if (!reply.isBlank()) {
                        saveMessage(sessionId, "assistant", reply, null, null);
                        sessionRepository.findById(sessionId).ifPresent(s -> updateSession(s, newCount));
                    }
                })
                // ── Critical: catch errors INSIDE the Flux so they never propagate to
                // Spring MVC's async error dispatcher. Without this, a failed AI call
                // triggers a security-contextless async dispatch that returns 401, which
                // causes the Angular client to log the user out. ────────────────────────
                .onErrorResume(e -> {
                    log.error("Streaming error for session {}: {}", sessionId, e.getMessage());
                    return Flux.just("[⚠️ AI service error: " + e.getMessage() + "]");
                });
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private String callChat(String userMessage, List<MessageRecord> history) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .messages(buildHistory(history))
                .user(userMessage)
                .call()
                .content();
    }

    private String handleFileAttachment(String userMessage, Long fileId, List<MessageRecord> history) {
        FileDocument doc = fileDocumentRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        if (doc.getContentType() != null && doc.getContentType().startsWith("image/")) {
            return analyzeImage(userMessage, doc);
        }

        String context = doc.getExtractedText() != null && !doc.getExtractedText().isBlank()
                ? "\n\n[Document content]\n" + truncate(doc.getExtractedText(), 6000)
                : "";

        return chatClient.prompt()
                .system(SYSTEM_PROMPT + (context.isEmpty() ? "" : " Use the document content below."))
                .messages(buildHistory(history))
                .user(userMessage + context)
                .call()
                .content();
    }

    private String analyzeImage(String userMessage, FileDocument doc) {
        try {
            byte[] imageBytes = fileStorageService.readBytes(doc.getStoragePath());
            MimeType mimeType = MimeTypeUtils.parseMimeType(doc.getContentType());
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes);

            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text(userMessage).media(mimeType, imageResource))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Image analysis failed for file {}, falling back to text chat: {}", doc.getId(), e.getMessage());
            return callChat(userMessage, new ArrayList<>());
        }
    }

    private String buildSystemContext(Long fileId) {
        if (fileId == null) return SYSTEM_PROMPT;
        return fileDocumentRepository.findById(fileId)
                .filter(d -> d.getExtractedText() != null && !d.getExtractedText().isBlank())
                .map(d -> SYSTEM_PROMPT + "\n\n[Document context]\n" + truncate(d.getExtractedText(), 6000))
                .orElse(SYSTEM_PROMPT);
    }

    private List<Message> buildHistory(List<MessageRecord> history) {
        int start = Math.max(0, history.size() - 20);
        return history.subList(start, history.size()).stream()
                .map(msg -> "user".equals(msg.getRole())
                        ? (Message) new UserMessage(msg.getContent())
                        : new AssistantMessage(msg.getContent()))
                .collect(Collectors.toList());
    }

    private ChatSession resolveSession(Long userId, ChatRequest request) {
        if (request.getSessionId() != null) {
            ChatSession session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
            if (!session.getUserId().equals(userId)) {
                throw new UnauthorizedException("Session does not belong to this user");
            }
            return session;
        }
        String title = request.getMessage().length() > 40
                ? request.getMessage().substring(0, 40) + "…"
                : request.getMessage();
        return sessionRepository.save(ChatSession.builder()
                .userId(userId)
                .title(title)
                .messageCount(0)
                .build());
    }

    private MessageRecord saveMessage(Long sessionId, String role, String content,
                                      Long fileId, String mimeType) {
        return messageRepository.save(MessageRecord.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .fileId(fileId)
                .mimeType(mimeType)
                .build());
    }

    private void updateSession(ChatSession session, int newCount) {
        session.setMessageCount(newCount);
        sessionRepository.save(session);
    }

    private void validateSessionOwnership(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new UnauthorizedException("Session does not belong to this user");
        }
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .messageCount(session.getMessageCount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(MessageRecord msg) {
        return MessageResponse.builder()
                .id(msg.getId())
                .sessionId(msg.getSessionId())
                .role(msg.getRole())
                .content(msg.getContent())
                .fileId(msg.getFileId())
                .mimeType(msg.getMimeType())
                .timestamp(msg.getTimestamp())
                .build();
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "\n[truncated]" : text;
    }
}
