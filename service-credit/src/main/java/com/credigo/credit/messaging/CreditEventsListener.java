package com.credigo.credit.messaging;

import com.credigo.credit.service.CreditService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consommateurs Kafka de service-credit (groupe service-credit), tous idempotents par eventId. */
@Component
public class CreditEventsListener {

    private final ConsommateurIdempotent consommateur;
    private final CreditService creditService;

    public CreditEventsListener(ConsommateurIdempotent consommateur, CreditService creditService) {
        this.consommateur = consommateur;
        this.creditService = creditService;
    }

    /** service-clients a calculé le score : DEPOSE -> EN_ETUDE (ou REFUSE si client non éligible). */
    @KafkaListener(topics = Topics.CLIENT_SCORED)
    public void surClientScore(String message) {
        consommateur.traiter(Topics.CLIENT_SCORED, message, creditService::surClientScore);
    }

    /** Fonds versés : ACCORDE -> DEBLOQUE, échéancier activé. */
    @KafkaListener(topics = Topics.DISBURSEMENT_PAID)
    public void surVersementEffectue(String message) {
        consommateur.traiter(Topics.DISBURSEMENT_PAID, message, creditService::surVersementEffectue);
    }

    /** Versement impossible : le dossier reste ACCORDE, décaissement à reprendre. */
    @KafkaListener(topics = Topics.DISBURSEMENT_FAILED)
    public void surVersementEchoue(String message) {
        consommateur.traiter(Topics.DISBURSEMENT_FAILED, message, creditService::surVersementEchoue);
    }
}
