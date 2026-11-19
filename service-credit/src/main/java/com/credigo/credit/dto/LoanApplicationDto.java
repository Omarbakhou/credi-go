package com.credigo.credit.dto;

import com.credigo.credit.enums.StatutDossier;

import java.math.BigDecimal;

public record LoanApplicationDto(
        Long id,
        Long clientId,
        BigDecimal montantDemande,
        BigDecimal montantAccorde,
        Integer scoreEtude,
        int dureeMois,
        BigDecimal tauxPropose,
        String motif,
        StatutDossier statut
) {
}
