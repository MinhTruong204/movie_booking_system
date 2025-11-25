package com.viecinema.common.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resourceName) {
        super(resourceName);
    }
}
