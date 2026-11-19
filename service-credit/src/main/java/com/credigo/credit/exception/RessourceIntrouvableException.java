package com.credigo.credit.exception;

/** Ressource inexistante : renvoie 404 via le GlobalExceptionHandler. */
public class RessourceIntrouvableException extends RuntimeException {
    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
