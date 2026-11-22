package com.credigo.credit.messaging.outbox;

import com.credigo.credit.messaging.EvenementMetier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** Écriture d'un événement dans l'outbox, au sein de la transaction métier en cours. */
@Service
public class OutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final String source;

    public OutboxService(OutboxRepository repository, ObjectMapper objectMapper,
                         @Value("${spring.application.name}") String source) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.source = source;
    }

    /**
     * Propagation MANDATORY : appeler cette méthode hors transaction est une erreur de programmation,
     * car l'événement doit être validé ou annulé avec l'opération métier.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public EvenementMetier enregistrer(String topic, String eventType, String businessId, long version,
                                       String correlationId, String causationId, Object donnees) {
        EvenementMetier evenement = new EvenementMetier(UUID.randomUUID().toString(), eventType, version,
                correlationId, causationId, businessId, source, Instant.now(), objectMapper.valueToTree(donnees));
        OutboxEvent ligne = new OutboxEvent();
        ligne.setEventId(evenement.eventId());
        ligne.setEventType(eventType);
        ligne.setTopic(topic);
        ligne.setCle(businessId);
        ligne.setCorrelationId(correlationId);
        ligne.setStatut(StatutOutbox.EN_ATTENTE);
        ligne.setDateCreation(LocalDateTime.now());
        try {
            ligne.setPayload(objectMapper.writeValueAsString(evenement));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Sérialisation de l'événement " + eventType + " impossible", e);
        }
        repository.save(ligne);
        return evenement;
    }
}
