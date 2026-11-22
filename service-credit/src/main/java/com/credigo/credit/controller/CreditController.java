package com.credigo.credit.controller;

import com.credigo.credit.dto.*;
import com.credigo.credit.enums.StatutDossier;
import com.credigo.credit.service.CreditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    /** GET /credits, /credits?clientId=1 ou /credits?statut=EN_ETUDE */
    @GetMapping
    public List<LoanApplicationDto> lister(@RequestParam(required = false) Long clientId,
                                           @RequestParam(required = false) StatutDossier statut) {
        return creditService.lister(clientId, statut);
    }

    @GetMapping("/{id}")
    public LoanApplicationDto consulter(@PathVariable Long id) {
        return creditService.consulter(id);
    }

    /** Consultation enrichie (client, garanties, échéancier, décaissements) depuis les projections locales. */
    @GetMapping("/{id}/details")
    public DossierDetailDto consulterDetail(@PathVariable Long id) {
        return creditService.consulterDetail(id);
    }

    @GetMapping("/{id}/echeancier")
    public List<RepaymentScheduleDto> echeancier(@PathVariable Long id) {
        return creditService.echeancier(id);
    }

    /**
     * 201 pour un nouveau dossier, 200 si la même Idempotency-Key a déjà été traitée.
     * V7 : le dépôt publie application-deposited ; le dossier passe EN_ETUDE à la réception du score.
     */
    @PostMapping
    public ResponseEntity<LoanApplicationDto> deposer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DemandeCreditDto dto) {
        ResultatDepot resultat = creditService.deposer(dto, idempotencyKey);
        if (!resultat.nouveau()) {
            return ResponseEntity.ok(resultat.dossier());
        }
        return ResponseEntity.created(URI.create("/credits/" + resultat.dossier().id())).body(resultat.dossier());
    }

    /** Modification d'une demande DEPOSE : nouvelle version, donc nouveau scoring. */
    @PutMapping("/{id}")
    public LoanApplicationDto modifier(@PathVariable Long id, @Valid @RequestBody DemandeCreditDto dto) {
        return creditService.modifier(id, dto);
    }

    /** Décision sur un dossier EN_ETUDE ; un accord publie loan-approved. */
    @PostMapping("/{id}/decision")
    public LoanApplicationDto decider(@PathVariable Long id) {
        return creditService.decider(id);
    }

    /** Annulation d'une demande DEPOSE ou EN_ETUDE : un score reçu ensuite ne la réactive pas. */
    @PostMapping("/{id}/annulation")
    public LoanApplicationDto annuler(@PathVariable Long id) {
        return creditService.annuler(id);
    }

    /** Relance du décaissement après un disbursement-failed (dossier ACCORDE, décaissement à reprendre). */
    @PostMapping("/{id}/relance-decaissement")
    public LoanApplicationDto relancerDecaissement(@PathVariable Long id) {
        return creditService.relancerDecaissement(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        creditService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
