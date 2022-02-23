package com.github.maksymiliank.arrivalpostofficeproxy;

public class PostmanException extends RuntimeException {

    public PostmanException(String message) {
        super(message);
    }

    public PostmanException(Exception e) {
        super(e);
    }
}
