package com.credigo.credit.projection;

import java.math.BigDecimal;

public record GarantieProjeteeDto(
        Long id,
        Long clientId,
        String type,
        BigDecimal valeur,
        String description
) {
}
