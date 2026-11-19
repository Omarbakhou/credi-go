package com.credigo.decaissements.dto;

import com.credigo.decaissements.enums.StatutDecaissement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DisbursementDto(
        Long id,
        Long loanApplicationId,
        Long treasuryAccountId,
        BigDecimal montant,
        LocalDateTime date,
        StatutDecaissement statut
) {
}
