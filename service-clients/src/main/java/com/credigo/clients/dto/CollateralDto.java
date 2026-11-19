package com.credigo.clients.dto;

import com.credigo.clients.enums.TypeGarantie;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CollateralDto(
        Long id,

        @NotNull(message = "L'identifiant du client est obligatoire")
        Long clientId,

        @NotNull(message = "Le type est obligatoire (CAUTION, VEHICULE, IMMOBILIER, EPARGNE)")
        TypeGarantie type,

        @NotNull(message = "La valeur est obligatoire")
        @Positive(message = "La valeur doit être strictement positive")
        BigDecimal valeur,

        @Size(max = 255)
        String description
) {
}
