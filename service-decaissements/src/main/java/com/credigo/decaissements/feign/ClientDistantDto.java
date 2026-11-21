package com.credigo.decaissements.feign;

import java.math.BigDecimal;

public record ClientDistantDto(
        Long id,
        String nom,
        String email,
        BigDecimal revenusMensuels,
        Integer scoreInterne,
        String statut
) {
    public boolean estActif() {
        return "ACTIF".equals(statut);
    }
}
