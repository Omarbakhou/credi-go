package com.credigo.credit.dto;

import com.credigo.credit.enums.StatutEcheance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RepaymentScheduleDto(
        Long id,
        Long loanApplicationId,
        int numeroEcheance,
        LocalDate dateEcheance,
        BigDecimal montantCapital,
        BigDecimal montantInterets,
        StatutEcheance statut
) {
}
