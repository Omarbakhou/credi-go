package com.credigo.credit.feign;

import java.math.BigDecimal;

/** Vue d'un client renvoyée par le service-clients (DTO dédié, découplé de l'entité distante). */
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
