package com.sandeep.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends AppException {
    public StorageException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
