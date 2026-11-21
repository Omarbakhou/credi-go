package com.credigo.credit.exception;

/**
 * Service distant injoignable (panne, timeout, circuit ouvert) : renvoie 503 via le
 * GlobalExceptionHandler. Levée par les méthodes de repli (fallback) de Resilience4j.
 */
public class ServiceExterneIndisponibleException extends RuntimeException {
    public ServiceExterneIndisponibleException(String message) {
        super(message);
    }
}
