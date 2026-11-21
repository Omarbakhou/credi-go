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

    /** Consultation enrichie (client, garanties, échéancier, décaissements). */
    @GetMapping("/{id}/details")
    public DossierDetailDto consulterDetail(@PathVariable Long id) {
        return creditService.consulterDetail(id);
    }

    @GetMapping("/{id}/echeancier")
    public List<RepaymentScheduleDto> echeancier(@PathVariable Long id) {
        return creditService.echeancier(id);
    }

    /** 201 pour un nouveau dossier, 200 si la même Idempotency-Key a déjà été traitée. */
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

    @PutMapping("/{id}")
    public LoanApplicationDto modifier(@PathVariable Long id, @Valid @RequestBody DemandeCreditDto dto) {
        return creditService.modifier(id, dto);
    }

    @PostMapping("/{id}/etude")
    public LoanApplicationDto etudier(@PathVariable Long id) {
        return creditService.etudier(id);
    }

    @PostMapping("/{id}/decision")
    public LoanApplicationDto decider(@PathVariable Long id) {
        return creditService.decider(id);
    }

    /** Appelé par service-decaissements après le versement effectif des fonds. */
    @PostMapping("/{id}/deblocage")
    public LoanApplicationDto debloquer(@PathVariable Long id) {
        return creditService.debloquer(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        creditService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
