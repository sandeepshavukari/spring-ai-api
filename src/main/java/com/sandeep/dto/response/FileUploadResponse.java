package com.sandeep.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResponse {

    private Long id;
    private String filename;
    private String contentType;
    private long size;
    private String extractedText;
    private boolean hasExtractedText;
    private LocalDateTime uploadedAt;
}
