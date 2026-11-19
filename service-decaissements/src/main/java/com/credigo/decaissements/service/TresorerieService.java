package com.credigo.decaissements.service;

import com.credigo.decaissements.dto.TreasuryAccountDto;
import com.credigo.decaissements.entite.TreasuryAccount;
import com.credigo.decaissements.exception.RegleMetierException;
import com.credigo.decaissements.exception.RessourceIntrouvableException;
import com.credigo.decaissements.mapper.DecaissementMapper;
import com.credigo.decaissements.repository.DisbursementRepository;
import com.credigo.decaissements.repository.TreasuryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TresorerieService {

    private final TreasuryAccountRepository compteRepository;
    private final DisbursementRepository disbursementRepository;
    private final DecaissementMapper mapper;

    public TresorerieService(TreasuryAccountRepository compteRepository,
                             DisbursementRepository disbursementRepository, DecaissementMapper mapper) {
        this.compteRepository = compteRepository;
        this.disbursementRepository = disbursementRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TreasuryAccountDto> lister() {
        return compteRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TreasuryAccountDto consulter(Long id) {
        return mapper.toDto(trouver(id));
    }

    public TreasuryAccountDto creer(TreasuryAccountDto dto) {
        if (compteRepository.existsByLibelle(dto.libelle())) {
            throw new RegleMetierException("Un compte de trésorerie existe déjà avec le libellé " + dto.libelle());
        }
        return mapper.toDto(compteRepository.save(mapper.toNouvelleEntite(dto)));
    }

    /** Seul le libellé est modifiable librement : le solde passe par les opérations prévues. */
    public TreasuryAccountDto modifier(Long id, TreasuryAccountDto dto) {
        TreasuryAccount compte = trouver(id);
        if (compteRepository.existsByLibelleAndIdNot(dto.libelle(), id)) {
            throw new RegleMetierException("Le libellé " + dto.libelle() + " est déjà utilisé");
        }
        compte.setLibelle(dto.libelle());
        return mapper.toDto(compte);
    }

    /** Opération métier : augmente le solde disponible. */
    public TreasuryAccountDto approvisionner(Long id, BigDecimal montant) {
        TreasuryAccount compte = trouver(id);
        compte.setSoldeDisponible(compte.getSoldeDisponible().add(montant));
        return mapper.toDto(compte);
    }

    /** Suppression refusée si le compte a déjà financé des décaissements (historique métier). */
    public void supprimer(Long id) {
        TreasuryAccount compte = trouver(id);
        if (disbursementRepository.existsByTreasuryAccountId(id)) {
            throw new RegleMetierException("Suppression impossible : le compte " + id
                    + " possède un historique de décaissements");
        }
        compteRepository.delete(compte);
    }

    TreasuryAccount trouver(Long id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte de trésorerie " + id + " introuvable"));
    }
}
