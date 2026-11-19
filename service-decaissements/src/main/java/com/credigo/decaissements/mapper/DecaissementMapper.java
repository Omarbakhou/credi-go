package com.credigo.decaissements.mapper;

import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.dto.TreasuryAccountDto;
import com.credigo.decaissements.entite.Disbursement;
import com.credigo.decaissements.entite.TreasuryAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DecaissementMapper {

    public TreasuryAccountDto toDto(TreasuryAccount compte) {
        return new TreasuryAccountDto(compte.getId(), compte.getLibelle(), compte.getSoldeDisponible());
    }

    public TreasuryAccount toNouvelleEntite(TreasuryAccountDto dto) {
        TreasuryAccount compte = new TreasuryAccount();
        compte.setLibelle(dto.libelle());
        compte.setSoldeDisponible(dto.soldeDisponible() == null ? BigDecimal.ZERO : dto.soldeDisponible());
        return compte;
    }

    public DisbursementDto toDto(Disbursement d) {
        return new DisbursementDto(d.getId(), d.getLoanApplicationId(), d.getTreasuryAccount().getId(),
                d.getMontant(), d.getDate(), d.getStatut());
    }
}
