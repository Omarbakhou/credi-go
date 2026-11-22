package com.credigo.credit.projection;

import com.credigo.credit.messaging.ConsommateurIdempotent;
import com.credigo.credit.messaging.EvenementMetier;
import com.credigo.credit.messaging.Topics;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Alimente la projection des clients depuis le topic compacté client-events.
 * Initialisation : à chaque attribution des partitions (donc à chaque démarrage), la lecture repart
 * du DÉBUT du topic ; la base H2 étant en mémoire, la projection est ainsi reconstruite à partir
 * du dernier état connu de chaque client. Groupe de consommateurs dédié à la projection.
 */
@Component
public class ProjectionClientsListener extends AbstractConsumerSeekAware {

    private static final Logger log = LoggerFactory.getLogger(ProjectionClientsListener.class);

    private final ProjectionClientService projectionService;
    private final ConsommateurIdempotent lecteur;

    public ProjectionClientsListener(ProjectionClientService projectionService, ConsommateurIdempotent lecteur) {
        this.projectionService = projectionService;
        this.lecteur = lecteur;
    }

    @KafkaListener(topics = Topics.CLIENT_EVENTS, groupId = "${spring.application.name}-projection-clients")
    public void surEvenementClient(String message) {
        EvenementMetier e = lecteur.lire(message);
        String resultat = projectionService.appliquer(e);
        log.debug("Projection du client {} : {}", e.businessId(), resultat);
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        callback.seekToBeginning(assignments.keySet());
        log.info("Projection des clients : relecture de client-events depuis le début ({} partitions)",
                assignments.size());
    }

    /** Reconstruction à la demande : vide la projection puis relit tout le topic. */
    public void reconstruire() {
        projectionService.vider();
        seekToBeginning();
        log.info("Projection des clients vidée : reconstruction depuis client-events demandée");
    }
}
