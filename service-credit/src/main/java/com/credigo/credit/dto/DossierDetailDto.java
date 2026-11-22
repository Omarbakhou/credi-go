package com.credigo.credit.dto;

import com.credigo.credit.projection.ClientProjeteDto;
import com.credigo.credit.projection.DecaissementProjeteDto;
import com.credigo.credit.projection.GarantieProjeteeDto;

import java.util.List;

/**
 * Consultation enrichie : dossier local + données des autres services lues dans les projections
 * locales (V7 : client et garanties via client-events, décaissements via disbursement-paid/failed).
 * Aucun appel distant : donneesPartielles vaut true si le client est absent de la projection.
 */
public record DossierDetailDto(
        LoanApplicationDto dossier,
        ClientProjeteDto client,
        List<GarantieProjeteeDto> garanties,
        List<RepaymentScheduleDto> echeancier,
        List<DecaissementProjeteDto> decaissements,
        boolean donneesPartielles
) {
}
