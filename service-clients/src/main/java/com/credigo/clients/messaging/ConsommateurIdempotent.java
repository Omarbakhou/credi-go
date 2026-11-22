package com.credigo.clients.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Function;

/**
 * Consommation idempotente d'un événement Kafka : dans UNE transaction locale,
 * on vérifie que l'eventId n'a pas déjà été traité, on applique le traitement métier,
 * puis on enregistre l'eventId dans la table evenement_traite.
 * En cas d'exception, tout est annulé et Kafka redélivre le message (retry puis .DLT).
 */
@Component
public class ConsommateurIdempotent {

    private static final Logger log = LoggerFactory.getLogger(ConsommateurIdempotent.class);

    private final EvenementTraiteRepository repository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaction;

    public ConsommateurIdempotent(EvenementTraiteRepository repository, ObjectMapper objectMapper,
                                  PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public EvenementMetier lire(String message) {
        try {
            EvenementMetier e = objectMapper.readValue(message, EvenementMetier.class);
            if (e.eventId() == null || e.data() == null) {
                throw new EvenementIllisibleException("Enveloppe incomplète (eventId ou data absent)", null);
            }
            return e;
        } catch (JsonProcessingException ex) {
            throw new EvenementIllisibleException("Message JSON illisible : " + ex.getOriginalMessage(), ex);
        }
    }

    public <T> T donnees(EvenementMetier e, Class<T> type) {
        try {
            return objectMapper.treeToValue(e.data(), type);
        } catch (JsonProcessingException ex) {
            throw new EvenementIllisibleException("Données " + e.eventType() + " illisibles", ex);
        }
    }

    /** traitement renvoie le résultat à tracer (ex. "DEBLOQUE" ou "IGNORE : résultat tardif"). */
    public void traiter(String topic, String message, Function<EvenementMetier, String> traitement) {
        EvenementMetier e = lire(message);
        transaction.executeWithoutResult(status -> {
            if (repository.existsById(e.eventId())) {
                log.info("Événement {} {} déjà traité : doublon ignoré", e.eventType(), e.eventId());
                return;
            }
            String resultat = traitement.apply(e);
            repository.save(new EvenementTraite(e, topic, resultat));
            log.info("Événement {} (dossier/clé {}, corrélation {}) : {}", e.eventType(), e.businessId(),
                    e.correlationId(), resultat);
        });
    }
}
