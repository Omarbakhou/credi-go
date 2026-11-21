package com.credigo.credit.dto;

import com.credigo.credit.feign.ClientDistantDto;
import com.credigo.credit.feign.DecaissementDistantDto;
import com.credigo.credit.feign.GarantieDistanteDto;

import java.util.List;

/**
 * Consultation enrichie (V4) : DTO composé du dossier local et des données agrégées
 * depuis service-clients et service-decaissements via OpenFeign.
 * client vaut null et les listes sont vides si un service distant est indisponible.
 */
public record DossierDetailDto(
        LoanApplicationDto dossier,
        ClientDistantDto client,
        List<GarantieDistanteDto> garanties,
        List<RepaymentScheduleDto> echeancier,
        List<DecaissementDistantDto> decaissements,
        boolean donneesPartielles
) {
}
