package com.credigo.decaissements.controller;

import com.credigo.decaissements.dto.DisbursementDto;
import com.credigo.decaissements.service.DecaissementService;
import org.springframework.web.bind.annotation.*;

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
}
