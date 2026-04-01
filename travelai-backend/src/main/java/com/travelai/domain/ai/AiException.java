package com.travelai.domain.ai;

public class AiException extends RuntimeException {
    public AiException(String msg) { super(msg); }
    public AiException(String msg, Throwable cause) { super(msg, cause); }
}
