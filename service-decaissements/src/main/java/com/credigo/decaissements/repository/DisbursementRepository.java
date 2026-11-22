package com.credigo.decaissements.repository;

import com.credigo.decaissements.entite.Disbursement;
import com.credigo.decaissements.enums.StatutDecaissement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface DisbursementRepository extends JpaRepository<Disbursement, Long> {

    boolean existsByTreasuryAccountId(Long treasuryAccountId);

    List<Disbursement> findByLoanApplicationIdOrderById(Long loanApplicationId);

    /** Un dossier n'a jamais plus d'un décaissement RESERVE ou VERSE. */
    boolean existsByLoanApplicationIdAndStatutIn(Long loanApplicationId, Collection<StatutDecaissement> statuts);

    List<Disbursement> findByStatutAndDateExpirationBefore(StatutDecaissement statut, LocalDateTime limite);
}
