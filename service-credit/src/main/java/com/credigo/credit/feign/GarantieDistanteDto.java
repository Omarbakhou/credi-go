package com.credigo.credit.feign;

import java.math.BigDecimal;

public record GarantieDistanteDto(
        Long id,
        Long clientId,
        String type,
        BigDecimal valeur,
        String description
) {
}
