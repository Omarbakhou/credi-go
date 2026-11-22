package com.credigo.clients.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Publication fiable : relit périodiquement les événements EN_ATTENTE (donc déjà validés par un commit)
 * et les envoie à Kafka (acks=all, producteur idempotent). Un événement n'est marqué PUBLIE qu'après
 * l'acquittement du broker ; en cas d'échec (Kafka arrêté, timeout), il reste EN_ATTENTE et sera
 * renvoyé au passage suivant. On s'arrête au premier échec pour conserver l'ordre d'émission.
 * Garantie obtenue : « au moins une fois », d'où les consommateurs idempotents.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository repository, KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${credigo.outbox.intervalle-ms:300}")
    public void publierEnAttente() {
        for (OutboxEvent ligne : repository.findTop50ByStatutOrderByIdAsc(StatutOutbox.EN_ATTENTE)) {
            try {
                kafkaTemplate.send(ligne.getTopic(), ligne.getCle(), ligne.getPayload()).get(10, TimeUnit.SECONDS);
                ligne.marquerPublie();
                repository.save(ligne);
                log.debug("Outbox : {} publié sur {} (clé {})", ligne.getEventType(), ligne.getTopic(), ligne.getCle());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
                ligne.marquerEchec(cause.getClass().getSimpleName() + " : " + cause.getMessage());
                repository.save(ligne);
                if (ligne.getTentatives() == 1 || ligne.getTentatives() % 20 == 0) {
                    log.warn("Outbox : publication de {} impossible (essai {}), nouvel essai au prochain passage : {}",
                            ligne.getEventType(), ligne.getTentatives(), cause.getMessage());
                }
                return;
            }
        }
    }
}
