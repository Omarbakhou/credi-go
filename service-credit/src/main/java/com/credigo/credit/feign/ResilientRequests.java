package com.credigo.credit.feign;

import com.credigo.credit.exception.RessourceIntrouvableException;
import com.credigo.credit.exception.ServiceExterneIndisponibleException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Appels OpenFeign protégés par Resilience4j (V6).
 * <ul>
 *   <li>Retry : 3 tentatives sur les erreurs transitoires (service injoignable, 503) ;</li>
 *   <li>Circuit Breaker : coupe les appels après trop d'échecs (les 4xx sont ignorés) ;</li>
 *   <li>Timeout : connectTimeout / readTimeout OpenFeign (config-repo).</li>
 * </ul>
 * Deux politiques de repli :
 * <ul>
 *   <li><b>stricte</b> (création, décision) : sans réponse fiable on refuse l'opération -> 503 ;</li>
 *   <li><b>tolérante</b> (consultation enrichie) : on renvoie une réponse partielle.</li>
 * </ul>
 */
@Component
public class ResilientRequests {

    private static final Logger log = LoggerFactory.getLogger(ResilientRequests.class);

    private final ClientsClient clientsClient;
    private final DecaissementsClient decaissementsClient;

    public ResilientRequests(ClientsClient clientsClient, DecaissementsClient decaissementsClient) {
        this.clientsClient = clientsClient;
        this.decaissementsClient = decaissementsClient;
    }

    // ===================== Politique stricte =====================

    @Retry(name = "service-clients", fallbackMethod = "clientIndisponible")
    @CircuitBreaker(name = "service-clients")
    public ClientDistantDto clientStrict(Long clientId) {
        return clientsClient.getClient(clientId);
    }

    @Retry(name = "service-clients", fallbackMethod = "garantiesIndisponibles")
    @CircuitBreaker(name = "service-clients")
    public List<GarantieDistanteDto> garantiesStrict(Long clientId) {
        return clientsClient.getGaranties(clientId);
    }

    // ===================== Politique tolérante =====================

    @Retry(name = "service-clients", fallbackMethod = "clientDeRepli")
    @CircuitBreaker(name = "service-clients")
    public ClientDistantDto clientTolerant(Long clientId) {
        return clientsClient.getClient(clientId);
    }

    @Retry(name = "service-clients", fallbackMethod = "garantiesDeRepli")
    @CircuitBreaker(name = "service-clients")
    public List<GarantieDistanteDto> garantiesTolerant(Long clientId) {
        return clientsClient.getGaranties(clientId);
    }

    @Retry(name = "service-decaissements", fallbackMethod = "decaissementsDeRepli")
    @CircuitBreaker(name = "service-decaissements")
    public List<DecaissementDistantDto> decaissementsTolerant(Long dossierId) {
        return decaissementsClient.getDecaissementsDuDossier(dossierId);
    }

    // ===================== Fallbacks =====================

    private ClientDistantDto clientIndisponible(Long clientId, Throwable ex) {
        traduireErreurMetier(ex, "Client " + clientId + " introuvable dans service-clients");
        throw new ServiceExterneIndisponibleException(
                "service-clients est momentanément indisponible. Réessayez dans quelques instants.");
    }

    private List<GarantieDistanteDto> garantiesIndisponibles(Long clientId, Throwable ex) {
        traduireErreurMetier(ex, "Client " + clientId + " introuvable dans service-clients");
        throw new ServiceExterneIndisponibleException(
                "service-clients est momentanément indisponible. Réessayez dans quelques instants.");
    }

    private ClientDistantDto clientDeRepli(Long clientId, Throwable ex) {
        traduireErreurMetier(ex, "Client " + clientId + " introuvable dans service-clients");
        log.warn("service-clients indisponible : client {} non enrichi", clientId);
        return null;
    }

    private List<GarantieDistanteDto> garantiesDeRepli(Long clientId, Throwable ex) {
        traduireErreurMetier(ex, "Client " + clientId + " introuvable dans service-clients");
        log.warn("service-clients indisponible : garanties du client {} non chargées", clientId);
        return List.of();
    }

    private List<DecaissementDistantDto> decaissementsDeRepli(Long dossierId, Throwable ex) {
        log.warn("service-decaissements indisponible : décaissements du dossier {} non chargés", dossierId);
        return List.of();
    }

    /**
     * Une erreur 4xx n'est pas une panne : 404 devient une ressource introuvable,
     * les autres erreurs clientes sont relancées telles quelles.
     */
    private void traduireErreurMetier(Throwable ex, String messageIntrouvable) {
        if (ex instanceof FeignException.NotFound) {
            throw new RessourceIntrouvableException(messageIntrouvable);
        }
        if (ex instanceof FeignException feign && feign.status() >= 400 && feign.status() < 500) {
            throw feign;
        }
        if (!(ex instanceof CallNotPermittedException) && !(ex instanceof FeignException)) {
            log.debug("Échec d'appel distant", ex);
        }
    }
}
