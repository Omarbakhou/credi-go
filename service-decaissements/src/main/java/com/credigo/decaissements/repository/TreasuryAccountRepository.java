package com.credigo.decaissements.repository;

import com.credigo.decaissements.entite.TreasuryAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

/**
 * Les mouvements de fonds sont des UPDATE conditionnels : contrôle du solde et mise à jour en une
 * seule instruction SQL, donc atomiques même si plusieurs décaissements arrivent en même temps.
 * Chaque requête renvoie 1 si le mouvement a eu lieu, 0 si la condition n'est pas remplie.
 */
public interface TreasuryAccountRepository extends JpaRepository<TreasuryAccount, Long> {

    boolean existsByLibelle(String libelle);

    boolean existsByLibelleAndIdNot(String libelle, Long id);

    /** Comptes candidats à une réservation, du plus approvisionné au moins approvisionné. */
    List<TreasuryAccount> findBySoldeDisponibleGreaterThanEqualOrderBySoldeDisponibleDescIdAsc(BigDecimal montant);

    /** Réservation : soldeDisponible -> montantReserve, si le solde est suffisant. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TreasuryAccount c set c.soldeDisponible = c.soldeDisponible - :montant, "
            + "c.montantReserve = c.montantReserve + :montant, c.version = c.version + 1 "
            + "where c.id = :id and c.soldeDisponible >= :montant")
    int reserver(Long id, BigDecimal montant);

    /** Versement effectif : les fonds réservés quittent la trésorerie. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TreasuryAccount c set c.montantReserve = c.montantReserve - :montant, c.version = c.version + 1 "
            + "where c.id = :id and c.montantReserve >= :montant")
    int debiterReserve(Long id, BigDecimal montant);

    /** Compensation : les fonds réservés et non versés redeviennent disponibles. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TreasuryAccount c set c.montantReserve = c.montantReserve - :montant, "
            + "c.soldeDisponible = c.soldeDisponible + :montant, c.version = c.version + 1 "
            + "where c.id = :id and c.montantReserve >= :montant")
    int liberer(Long id, BigDecimal montant);
}
