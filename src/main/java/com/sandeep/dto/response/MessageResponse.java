package com.sandeep.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageResponse {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private Long fileId;
    private String mimeType;
    private LocalDateTime timestamp;
}
