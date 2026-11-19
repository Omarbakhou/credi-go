package com.credigo.clients.controller;

import com.credigo.clients.dto.CollateralDto;
import com.credigo.clients.service.CollateralService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/garanties")
public class CollateralController {

    private final CollateralService collateralService;

    public CollateralController(CollateralService collateralService) {
        this.collateralService = collateralService;
    }

    @GetMapping
    public List<CollateralDto> lister() {
        return collateralService.lister();
    }

    @GetMapping("/{id}")
    public CollateralDto consulter(@PathVariable Long id) {
        return collateralService.consulter(id);
    }

    @PostMapping
    public ResponseEntity<CollateralDto> creer(@Valid @RequestBody CollateralDto dto) {
        CollateralDto cree = collateralService.creer(dto);
        return ResponseEntity.created(URI.create("/garanties/" + cree.id())).body(cree);
    }

    @PutMapping("/{id}")
    public CollateralDto modifier(@PathVariable Long id, @Valid @RequestBody CollateralDto dto) {
        return collateralService.modifier(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        collateralService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
