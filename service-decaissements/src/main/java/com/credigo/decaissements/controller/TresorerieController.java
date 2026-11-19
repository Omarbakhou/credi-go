package com.credigo.decaissements.controller;

import com.credigo.decaissements.dto.ApprovisionnementDto;
import com.credigo.decaissements.dto.TreasuryAccountDto;
import com.credigo.decaissements.service.TresorerieService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tresoreries")
public class TresorerieController {

    private final TresorerieService tresorerieService;

    public TresorerieController(TresorerieService tresorerieService) {
        this.tresorerieService = tresorerieService;
    }

    @GetMapping
    public List<TreasuryAccountDto> lister() {
        return tresorerieService.lister();
    }

    @GetMapping("/{id}")
    public TreasuryAccountDto consulter(@PathVariable Long id) {
        return tresorerieService.consulter(id);
    }

    @PostMapping
    public ResponseEntity<TreasuryAccountDto> creer(@Valid @RequestBody TreasuryAccountDto dto) {
        TreasuryAccountDto cree = tresorerieService.creer(dto);
        return ResponseEntity.created(URI.create("/tresoreries/" + cree.id())).body(cree);
    }

    @PutMapping("/{id}")
    public TreasuryAccountDto modifier(@PathVariable Long id, @Valid @RequestBody TreasuryAccountDto dto) {
        return tresorerieService.modifier(id, dto);
    }

    @PostMapping("/{id}/approvisionnement")
    public TreasuryAccountDto approvisionner(@PathVariable Long id, @Valid @RequestBody ApprovisionnementDto dto) {
        return tresorerieService.approvisionner(id, dto.montant());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        tresorerieService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
