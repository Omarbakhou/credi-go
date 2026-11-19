package com.credigo.clients.controller;

import com.credigo.clients.dto.ClientDto;
import com.credigo.clients.dto.CollateralDto;
import com.credigo.clients.enums.StatutClient;
import com.credigo.clients.service.ClientService;
import com.credigo.clients.service.CollateralService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final CollateralService collateralService;

    public ClientController(ClientService clientService, CollateralService collateralService) {
        this.clientService = clientService;
        this.collateralService = collateralService;
    }

    @GetMapping
    public List<ClientDto> lister() {
        return clientService.lister();
    }

    @GetMapping("/{id}")
    public ClientDto consulter(@PathVariable Long id) {
        return clientService.consulter(id);
    }

    @GetMapping("/{id}/garanties")
    public List<CollateralDto> garantiesDuClient(@PathVariable Long id) {
        return collateralService.listerParClient(id);
    }

    @PostMapping
    public ResponseEntity<ClientDto> creer(@Valid @RequestBody ClientDto dto) {
        ClientDto cree = clientService.creer(dto);
        return ResponseEntity.created(URI.create("/clients/" + cree.id())).body(cree);
    }

    @PutMapping("/{id}")
    public ClientDto modifier(@PathVariable Long id, @Valid @RequestBody ClientDto dto) {
        return clientService.modifier(id, dto);
    }

    @PostMapping("/{id}/activation")
    public ClientDto activer(@PathVariable Long id) {
        return clientService.changerStatut(id, StatutClient.ACTIF);
    }

    @PostMapping("/{id}/desactivation")
    public ClientDto desactiver(@PathVariable Long id) {
        return clientService.changerStatut(id, StatutClient.INACTIF);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        clientService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
