package com.credigo.decaissements.feign;

import java.math.BigDecimal;

/** Vue d'un dossier renvoyée par le service-credit. */
public record DossierDistantDto(
        Long id,
        Long clientId,
        BigDecimal montantDemande,
        BigDecimal montantAccorde,
        Integer scoreEtude,
        int dureeMois,
        BigDecimal tauxPropose,
        String motif,
        String commentaireDecision,
        String statut
) {
}
