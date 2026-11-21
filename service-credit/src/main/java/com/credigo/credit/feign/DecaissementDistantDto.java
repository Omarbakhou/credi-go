package com.credigo.credit.feign;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DecaissementDistantDto(
        Long id,
        Long loanApplicationId,
        Long treasuryAccountId,
        BigDecimal montant,
        LocalDateTime date,
        String statut
) {
}
