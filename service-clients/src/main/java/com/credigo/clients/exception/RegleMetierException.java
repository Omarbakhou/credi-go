package com.credigo.clients.exception;

/**
 * Opération refusée par une règle métier (doublon, suppression impossible, transition
 * de statut interdite...) : renvoie 409 Conflict via le GlobalExceptionHandler.
 */
public class RegleMetierException extends RuntimeException {
    public RegleMetierException(String message) {
        super(message);
    }
}
