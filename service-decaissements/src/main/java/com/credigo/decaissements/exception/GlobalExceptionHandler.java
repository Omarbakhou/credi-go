package com.credigo.decaissements.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gestion centralisée des erreurs : chaque exception est traduite en code HTTP adapté
 * avec un corps JSON homogène ({@link ErreurResponse}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurResponse> introuvable(RessourceIntrouvableException ex, HttpServletRequest req) {
        return reponse(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<ErreurResponse> regleMetier(RegleMetierException ex, HttpServletRequest req) {
        return reponse(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ServiceExterneIndisponibleException.class)
    public ResponseEntity<ErreurResponse> indisponible(ServiceExterneIndisponibleException ex, HttpServletRequest req) {
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req, null);
    }

    /** Échec de @Valid sur le corps de la requête : liste des champs invalides. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> champs = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            champs.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return reponse(HttpStatus.BAD_REQUEST, "Données invalides", req, champs);
    }

    /** JSON mal formé ou valeur d'énumération inconnue. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurResponse> illisible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return reponse(HttpStatus.BAD_REQUEST, "Corps de requête illisible ou valeur inconnue", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurResponse> typeInvalide(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return reponse(HttpStatus.BAD_REQUEST, "Paramètre invalide : " + ex.getName(), req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErreurResponse> integrite(DataIntegrityViolationException ex, HttpServletRequest req) {
        return reponse(HttpStatus.CONFLICT, "Contrainte d'intégrité violée", req, null);
    }

    /** Modification concurrente détectée par le verrou optimiste (@Version) : à rejouer. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErreurResponse> concurrence(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return reponse(HttpStatus.CONFLICT, "Modification concurrente de la ressource : réessayez", req, null);
    }

    private ResponseEntity<ErreurResponse> reponse(HttpStatus status, String message,
                                                   HttpServletRequest req, Map<String, String> details) {
        ErreurResponse corps = new ErreurResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, req.getRequestURI(), details);
        return ResponseEntity.status(status).body(corps);
    }
}
