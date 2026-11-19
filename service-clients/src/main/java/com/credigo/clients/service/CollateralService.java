package com.credigo.clients.service;

import com.credigo.clients.dto.CollateralDto;
import com.credigo.clients.entite.Client;
import com.credigo.clients.entite.Collateral;
import com.credigo.clients.enums.StatutClient;
import com.credigo.clients.exception.RegleMetierException;
import com.credigo.clients.exception.RessourceIntrouvableException;
import com.credigo.clients.mapper.ClientMapper;
import com.credigo.clients.repository.CollateralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CollateralService {

    private final CollateralRepository collateralRepository;
    private final ClientService clientService;
    private final ClientMapper mapper;

    public CollateralService(CollateralRepository collateralRepository, ClientService clientService,
                             ClientMapper mapper) {
        this.collateralRepository = collateralRepository;
        this.clientService = clientService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CollateralDto> lister() {
        return collateralRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CollateralDto> listerParClient(Long clientId) {
        clientService.trouver(clientId); // 404 si le client n'existe pas
        return collateralRepository.findByClientId(clientId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CollateralDto consulter(Long id) {
        return mapper.toDto(trouver(id));
    }

    public CollateralDto creer(CollateralDto dto) {
        Client client = clientActif(dto.clientId());
        Collateral garantie = new Collateral();
        mapper.copier(dto, garantie, client);
        return mapper.toDto(collateralRepository.save(garantie));
    }

    public CollateralDto modifier(Long id, CollateralDto dto) {
        Collateral garantie = trouver(id);
        Client client = clientActif(dto.clientId());
        mapper.copier(dto, garantie, client);
        return mapper.toDto(garantie);
    }

    public void supprimer(Long id) {
        collateralRepository.delete(trouver(id));
    }

    /** Référence locale validée : le client doit exister et être actif. */
    private Client clientActif(Long clientId) {
        Client client = clientService.trouver(clientId);
        if (client.getStatut() != StatutClient.ACTIF) {
            throw new RegleMetierException("Le client " + clientId + " est inactif : garantie refusée");
        }
        return client;
    }

    private Collateral trouver(Long id) {
        return collateralRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Garantie " + id + " introuvable"));
    }
}
