package com.credigo.credit.controller;

import com.credigo.credit.dto.DemandeCreditDto;
import com.credigo.credit.dto.LoanApplicationDto;
import com.credigo.credit.dto.RepaymentScheduleDto;
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

    @GetMapping("/{id}/echeancier")
    public List<RepaymentScheduleDto> echeancier(@PathVariable Long id) {
        return creditService.echeancier(id);
    }

    @PostMapping
    public ResponseEntity<LoanApplicationDto> deposer(@Valid @RequestBody DemandeCreditDto dto) {
        LoanApplicationDto cree = creditService.deposer(dto);
        return ResponseEntity.created(URI.create("/credits/" + cree.id())).body(cree);
    }

    @PutMapping("/{id}")
    public LoanApplicationDto modifier(@PathVariable Long id, @Valid @RequestBody DemandeCreditDto dto) {
        return creditService.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        creditService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
