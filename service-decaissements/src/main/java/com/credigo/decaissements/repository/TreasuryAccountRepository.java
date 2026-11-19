package com.credigo.decaissements.repository;

import com.credigo.decaissements.entite.TreasuryAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount, Long> {

    boolean existsByLibelle(String libelle);

    boolean existsByLibelleAndIdNot(String libelle, Long id);
}
