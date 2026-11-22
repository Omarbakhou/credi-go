package com.credigo.credit.dto;

import com.credigo.credit.enums.StatutDossier;

import java.math.BigDecimal;

/** V7 : correlationId du processus, état du décaissement (à reprendre après un échec). */
public record LoanApplicationDto(
        Long id,
        Long clientId,
        BigDecimal montantDemande,
        BigDecimal montantAccorde,
        Integer scoreEtude,
        int dureeMois,
        BigDecimal tauxPropose,
        String motif,
        String commentaireDecision,
        StatutDossier statut,
        String correlationId,
        int tentativeDecaissement,
        boolean decaissementAReprendre,
        String motifEchecDecaissement
) {
}
