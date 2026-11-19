package com.credigo.credit.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Corps d'une demande de crédit (création ou modification tant que le dossier est DEPOSE). */
public record DemandeCreditDto(
        @NotNull(message = "L'identifiant du client est obligatoire")
        Long clientId,

        @NotNull(message = "Le montant demandé est obligatoire")
        @DecimalMin(value = "1000.00", message = "Montant minimum : 1 000 MAD")
        @DecimalMax(value = "500000.00", message = "Montant maximum : 500 000 MAD")
        BigDecimal montantDemande,

        @NotNull(message = "La durée est obligatoire")
        @Min(value = 6, message = "Durée minimum : 6 mois")
        @Max(value = 84, message = "Durée maximum : 84 mois")
        Integer dureeMois,

        @Size(max = 255)
        String motif
) {
}
