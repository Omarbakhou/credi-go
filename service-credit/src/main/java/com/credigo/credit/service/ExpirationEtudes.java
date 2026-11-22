package com.credigo.credit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Timeout du scoring (V7) : une demande restée DEPOSE plus longtemps que credigo.etude.delai-max
 * (aucun client-scored reçu) est annulée. Un score qui arriverait ensuite est ignoré (résultat tardif).
 */
@Component
public class ExpirationEtudes {

    private static final Logger log = LoggerFactory.getLogger(ExpirationEtudes.class);

    private final CreditService creditService;
    private final Duration delaiMax;

    public ExpirationEtudes(CreditService creditService,
                            @Value("${credigo.etude.delai-max:PT5M}") Duration delaiMax) {
        this.creditService = creditService;
        this.delaiMax = delaiMax;
    }

    @Scheduled(fixedDelayString = "${credigo.etude.verification-ms:10000}")
    public void expirer() {
        int n = creditService.expirerEtudes(LocalDateTime.now().minus(delaiMax));
        if (n > 0) {
            log.warn("{} demande(s) annulée(s) : aucun score reçu en {}", n, delaiMax);
        }
    }
}
