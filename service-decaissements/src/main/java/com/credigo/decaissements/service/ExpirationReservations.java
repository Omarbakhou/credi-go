package com.credigo.decaissements.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Timeout (V7) : libère les fonds des réservations non versées dans le délai (credigo.decaissements.delai-versement). */
@Component
public class ExpirationReservations {

    private static final Logger log = LoggerFactory.getLogger(ExpirationReservations.class);

    private final DecaissementService decaissementService;

    public ExpirationReservations(DecaissementService decaissementService) {
        this.decaissementService = decaissementService;
    }

    @Scheduled(fixedDelayString = "${credigo.decaissements.verification-ms:10000}")
    public void expirer() {
        int n = decaissementService.expirerReservations();
        if (n > 0) {
            log.warn("{} réservation(s) expirée(s) : fonds libérés, disbursement-failed publié", n);
        }
    }
}
