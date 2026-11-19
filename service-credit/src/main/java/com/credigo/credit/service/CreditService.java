package com.credigo.credit.service;

import com.credigo.credit.dto.DemandeCreditDto;
import com.credigo.credit.dto.LoanApplicationDto;
import com.credigo.credit.dto.RepaymentScheduleDto;
import com.credigo.credit.entite.LoanApplication;
import com.credigo.credit.enums.StatutDossier;
import com.credigo.credit.exception.RegleMetierException;
import com.credigo.credit.exception.RessourceIntrouvableException;
import com.credigo.credit.mapper.CreditMapper;
import com.credigo.credit.repository.LoanApplicationRepository;
import com.credigo.credit.repository.RepaymentScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Jalon V1 : CRUD local du dossier de crédit. La vérification du client via OpenFeign,
 * l'étude, la décision et l'activation de l'échéancier sont ajoutées au jalon V4.
 */
@Service
@Transactional
public class CreditService {

    private final LoanApplicationRepository dossierRepository;
    private final RepaymentScheduleRepository echeanceRepository;
    private final CreditMapper mapper;

    public CreditService(LoanApplicationRepository dossierRepository,
                         RepaymentScheduleRepository echeanceRepository, CreditMapper mapper) {
        this.dossierRepository = dossierRepository;
        this.echeanceRepository = echeanceRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationDto> lister(Long clientId, StatutDossier statut) {
        List<LoanApplication> dossiers;
        if (clientId != null) {
            dossiers = dossierRepository.findByClientId(clientId);
        } else if (statut != null) {
            dossiers = dossierRepository.findByStatut(statut);
        } else {
            dossiers = dossierRepository.findAll();
        }
        return dossiers.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LoanApplicationDto consulter(Long id) {
        return mapper.toDto(trouver(id));
    }

    /** Dépôt d'une demande : le dossier démarre toujours au statut DEPOSE. */
    public LoanApplicationDto deposer(DemandeCreditDto dto) {
        LoanApplication dossier = new LoanApplication();
        mapper.copierDemande(dto, dossier);
        dossier.setStatut(StatutDossier.DEPOSE);
        return mapper.toDto(dossierRepository.save(dossier));
    }

    /** La demande n'est modifiable que tant que l'étude n'a pas commencé. */
    public LoanApplicationDto modifier(Long id, DemandeCreditDto dto) {
        LoanApplication dossier = trouver(id);
        if (dossier.getStatut() != StatutDossier.DEPOSE) {
            throw new RegleMetierException("Le dossier " + id + " est " + dossier.getStatut()
                    + " : seule une demande DEPOSE peut être modifiée");
        }
        mapper.copierDemande(dto, dossier);
        return mapper.toDto(dossier);
    }

    /** Suppression autorisée uniquement pour un dossier DEPOSE ou REFUSE (pas d'historique financier). */
    public void supprimer(Long id) {
        LoanApplication dossier = trouver(id);
        if (dossier.getStatut() != StatutDossier.DEPOSE && dossier.getStatut() != StatutDossier.REFUSE) {
            throw new RegleMetierException("Suppression impossible : le dossier " + id + " est "
                    + dossier.getStatut());
        }
        dossierRepository.delete(dossier);
    }

    @Transactional(readOnly = true)
    public List<RepaymentScheduleDto> echeancier(Long id) {
        trouver(id); // 404 si le dossier n'existe pas
        return echeanceRepository.findByLoanApplicationIdOrderByNumeroEcheance(id).stream()
                .map(mapper::toDto).toList();
    }

    LoanApplication trouver(Long id) {
        return dossierRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Dossier de crédit " + id + " introuvable"));
    }
}
