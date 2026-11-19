package com.credigo.decaissements.exception;

import java.time.LocalDateTime;
import java.util.Map;

/** Format unique des erreurs renvoyées par l'API. */
public record ErreurResponse(
        LocalDateTime timestamp,
        int status,
        String erreur,
        String message,
        String chemin,
        Map<String, String> details
) {
}
