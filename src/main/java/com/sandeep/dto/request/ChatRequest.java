package com.sandeep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 10000, message = "Message too long")
    private String message;

    private Long sessionId;

    private Long fileId;
}
