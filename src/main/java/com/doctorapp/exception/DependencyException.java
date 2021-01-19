package com.doctorapp.exception;

/**
 * This exception is thrown on dependency error
 */
public class DependencyException extends RuntimeException {

    public DependencyException(String message) {
        super(message);
    }

    public DependencyException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
