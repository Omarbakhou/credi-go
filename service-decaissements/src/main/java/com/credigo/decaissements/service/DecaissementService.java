package com.credigo.decaissements.service;

import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.exception.RessourceIntrouvableException;
import com.credigo.decaissements.mapper.DecaissementMapper;
import com.credigo.decaissements.repository.DisbursementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consultation des décaissements. La création (réservation puis débit de la trésorerie
 * pour un dossier ACCORDE vérifié via OpenFeign) est ajoutée au jalon V4.
 */
@Service
@Transactional(readOnly = true)
public class DecaissementService {

    private final DisbursementRepository disbursementRepository;
    private final DecaissementMapper mapper;

    public DecaissementService(DisbursementRepository disbursementRepository, DecaissementMapper mapper) {
        this.disbursementRepository = disbursementRepository;
        this.mapper = mapper;
    }

    public List<DisbursementDto> lister() {
        return disbursementRepository.findAll().stream().map(mapper::toDto).toList();
    }

    public DisbursementDto consulter(Long id) {
        return disbursementRepository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new RessourceIntrouvableException("Décaissement " + id + " introuvable"));
    }

    public List<DisbursementDto> listerParDossier(Long loanApplicationId) {
        return disbursementRepository.findByLoanApplicationId(loanApplicationId).stream()
                .map(mapper::toDto).toList();
    }
}
