package com.sandeep.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Slf4j
public class DocumentExtractionService {

    private final AutoDetectParser parser = new AutoDetectParser();

    public String extractText(InputStream inputStream, String contentType) {
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            if (contentType != null) {
                metadata.set(Metadata.CONTENT_TYPE, contentType);
            }
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = handler.toString().trim();
            log.debug("Extracted {} characters from document", text.length());
            return text;
        } catch (Exception e) {
            log.warn("Text extraction failed for content type {}: {}", contentType, e.getMessage());
            return "";
        }
    }

    public boolean isExtractableDocument(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith("application/pdf")
                || contentType.startsWith("application/msword")
                || contentType.contains("officedocument")
                || contentType.startsWith("text/")
                || contentType.startsWith("application/rtf")
                || contentType.equals("application/epub+zip");
    }

    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
