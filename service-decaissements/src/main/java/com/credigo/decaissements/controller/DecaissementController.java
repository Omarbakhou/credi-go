package com.credigo.decaissements.controller;

import com.credigo.decaissements.dto.DemandeDecaissementDto;
import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.dto.ResultatDecaissement;
import com.credigo.decaissements.service.DecaissementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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

    /** Versement : 201 pour un nouveau versement, 200 si la même Idempotency-Key a déjà été traitée. */
    @PostMapping
    public ResponseEntity<DisbursementDto> verser(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DemandeDecaissementDto demande) {
        ResultatDecaissement resultat = decaissementService.verser(demande, idempotencyKey);
        if (!resultat.nouveau()) {
            return ResponseEntity.ok(resultat.decaissement());
        }
        return ResponseEntity.created(URI.create("/decaissements/" + resultat.decaissement().id()))
                .body(resultat.decaissement());
    }
}
