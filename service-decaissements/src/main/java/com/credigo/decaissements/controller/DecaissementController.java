package com.credigo.decaissements.controller;

import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.service.DecaissementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V7 : les décaissements sont créés (RESERVE) à la réception de loan-approved, et non plus par POST.
 * Le versement effectif et l'annulation sont des opérations sur un décaissement existant.
 */
@RestController
@RequestMapping("/decaissements")
public class DecaissementController {

    private final DecaissementService decaissementService;

    public DecaissementController(DecaissementService decaissementService) {
        this.decaissementService = decaissementService;
    }

    /** GET /decaissements ou GET /decaissements?dossierId=3 */
    @GetMapping
    public List<DisbursementDto> lister(@RequestParam(required = false) Long dossierId) {
        return dossierId == null ? decaissementService.lister() : decaissementService.listerParDossier(dossierId);
    }

    @GetMapping("/{id}")
    public DisbursementDto consulter(@PathVariable Long id) {
        return decaissementService.consulter(id);
    }

    /** Versement des fonds réservés : VERSE + disbursement-paid. Idempotent (200 si déjà versé). */
    @PostMapping("/{id}/versement")
    public DisbursementDto verser(@PathVariable Long id) {
        return decaissementService.verser(id);
    }

    /** Annulation d'une réservation non versée : fonds libérés + disbursement-failed. */
    @PostMapping("/{id}/annulation")
    public DisbursementDto annuler(@PathVariable Long id) {
        return decaissementService.annuler(id);
    }
}
