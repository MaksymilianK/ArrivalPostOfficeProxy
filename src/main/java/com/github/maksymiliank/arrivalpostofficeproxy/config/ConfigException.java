package com.github.maksymiliank.arrivalpostofficeproxy.config;

public class ConfigException extends RuntimeException {

    public ConfigException(String message) {
        super(message);
    }

    public ConfigException(Exception e) {
        super(e);
    }
}
