package com.credigo.decaissements.dto;

import com.credigo.decaissements.enums.StatutDecaissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** date : date du versement effectif ; dateReservation / dateExpiration : préparation (V7). */
public record DisbursementDto(
        Long id,
        Long loanApplicationId,
        Long treasuryAccountId,
        BigDecimal montant,
        LocalDateTime date,
        StatutDecaissement statut,
        Long clientId,
        int tentative,
        LocalDateTime dateReservation,
        LocalDateTime dateExpiration,
        String motifEchec,
        String correlationId
) {
}
