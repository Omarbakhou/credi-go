package com.credigo.credit.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DecaissementProjeteDto(
        Long id,
        Long loanApplicationId,
        Long treasuryAccountId,
        BigDecimal montant,
        LocalDateTime date,
        String statut,
        String motif
) {
}
