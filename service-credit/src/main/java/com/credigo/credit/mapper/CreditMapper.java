package com.credigo.credit.mapper;

import com.credigo.credit.dto.DemandeCreditDto;
import com.credigo.credit.dto.LoanApplicationDto;
import com.credigo.credit.dto.RepaymentScheduleDto;
import com.credigo.credit.entite.LoanApplication;
import com.credigo.credit.entite.RepaymentSchedule;
import org.springframework.stereotype.Component;

@Component
public class CreditMapper {

    public LoanApplicationDto toDto(LoanApplication d) {
        return new LoanApplicationDto(d.getId(), d.getClientId(), d.getMontantDemande(), d.getMontantAccorde(),
                d.getScoreEtude(), d.getDureeMois(), d.getTauxPropose(), d.getMotif(), d.getStatut());
    }

    /** Copie la demande dans le dossier (création, ou modification d'un dossier DEPOSE). */
    public void copierDemande(DemandeCreditDto dto, LoanApplication dossier) {
        dossier.setClientId(dto.clientId());
        dossier.setMontantDemande(dto.montantDemande());
        dossier.setDureeMois(dto.dureeMois());
        dossier.setMotif(dto.motif());
    }

    public RepaymentScheduleDto toDto(RepaymentSchedule e) {
        return new RepaymentScheduleDto(e.getId(), e.getLoanApplication().getId(), e.getNumeroEcheance(),
                e.getDateEcheance(), e.getMontantCapital(), e.getMontantInterets(), e.getStatut());
    }
}
