package com.credigo.credit.projection;

import com.credigo.credit.messaging.ConsommateurIdempotent;
import com.credigo.credit.messaging.EvenementMetier;
import com.credigo.credit.messaging.Topics;
import com.credigo.credit.messaging.donnees.ClientDonnees;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Projection locale des clients et de leurs garanties (V7).
 * Application idempotente par version : un événement dont la version n'est pas plus récente que la
 * projection est ignoré. Rejouer tout le topic (reconstruction) redonne donc exactement le même état.
 */
@Service
@Transactional
public class ProjectionClientService {

    private final ClientProjectionRepository clientRepository;
    private final GarantieProjectionRepository garantieRepository;
    private final ConsommateurIdempotent lecteur;

    public ProjectionClientService(ClientProjectionRepository clientRepository,
                                   GarantieProjectionRepository garantieRepository, ConsommateurIdempotent lecteur) {
        this.clientRepository = clientRepository;
        this.garantieRepository = garantieRepository;
        this.lecteur = lecteur;
    }

    public String appliquer(EvenementMetier e) {
        ClientDonnees donnees = lecteur.donnees(e, ClientDonnees.class);
        Long clientId = donnees.id() != null ? donnees.id() : Long.valueOf(e.businessId());
        Optional<ClientProjection> existante = clientRepository.findById(clientId);
        if (existante.isPresent() && e.version() <= existante.get().getVersion()) {
            return "IGNORE : version " + e.version() + " déjà appliquée (projection en version "
                    + existante.get().getVersion() + ")";
        }
        ClientProjection projection = existante.orElseGet(() -> new ClientProjection(clientId));
        projection.setVersion(e.version());
        projection.setDateMiseAJour(LocalDateTime.now());
        garantieRepository.supprimerDuClient(clientId);
        if (Topics.TYPE_CLIENT_DELETED.equals(e.eventType())) {
            projection.setSupprime(true);
            clientRepository.save(projection);
            return "CLIENT " + clientId + " SUPPRIME";
        }
        projection.setSupprime(false);
        projection.setNom(donnees.nom());
        projection.setEmail(donnees.email());
        projection.setRevenusMensuels(donnees.revenusMensuels());
        projection.setScoreInterne(donnees.scoreInterne());
        projection.setStatut(donnees.statut());
        clientRepository.save(projection);
        if (donnees.garanties() != null) {
            donnees.garanties().forEach(g -> garantieRepository.save(
                    new GarantieProjection(g.id(), clientId, g.type(), g.valeur(), g.description())));
        }
        return "CLIENT " + clientId + " " + donnees.statut() + " (version " + e.version() + ")";
    }

    /** Client connu et non supprimé dans la projection locale. */
    @Transactional(readOnly = true)
    public Optional<ClientProjection> client(Long clientId) {
        return clientRepository.findById(clientId).filter(c -> !c.isSupprime());
    }

    @Transactional(readOnly = true)
    public List<GarantieProjeteeDto> garanties(Long clientId) {
        return garantieRepository.findByClientIdOrderById(clientId).stream()
                .map(g -> new GarantieProjeteeDto(g.getId(), g.getClientId(), g.getType(), g.getValeur(),
                        g.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClientProjeteDto> lister() {
        return clientRepository.findAll().stream().filter(c -> !c.isSupprime())
                .map(ProjectionClientService::toDto).toList();
    }

    /** Vide la projection avant de rejouer le topic client-events depuis le début. */
    public void vider() {
        garantieRepository.deleteAllInBatch();
        clientRepository.deleteAllInBatch();
    }

    public static ClientProjeteDto toDto(ClientProjection c) {
        return new ClientProjeteDto(c.getClientId(), c.getNom(), c.getEmail(), c.getRevenusMensuels(),
                c.getScoreInterne(), c.getStatut());
    }
}
