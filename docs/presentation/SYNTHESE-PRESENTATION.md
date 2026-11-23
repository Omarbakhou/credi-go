# Credi-Go — Synthèse pour la présentation (Contrôle 1ère évaluation)

Module : J2EE Avancée et Spring Boot (5ème année IDSI, EMSI) — Fiche projet n°22.
Repo : https://github.com/Omarbakhou/credi-go

## 1. Le projet en 3 phrases

- **Credi-Go** = plateforme d'octroi de crédits à la consommation en micro-services.
- Unité de traitement : le **Dossier de crédit** (`LoanApplication`) : 1 demandeur + 1 montant + 1 durée + 1 taux + 1 statut.
- Cycle de vie : `DEPOSE → EN_ETUDE → ACCORDE → DEBLOQUE` (ou `REFUSE`).

## 2. Stack

Java 17 · Spring Boot 3.5.6 · Spring Cloud 2025.0.0 (Eureka, Gateway, Config Server, OpenFeign, Resilience4j) · RabbitMQ · MailHog · H2 (une base par service) · Maven multi-modules · Postman.

## 3. Les 7 applications

| Application | Port | Rôle | Entités |
|---|---|---|---|
| service-registry | 8761 | Eureka Server (annuaire) | — |
| config-server | 8888 | Configuration centralisée (`config-repo/`) | — |
| gateway-service | 8080 | API Gateway (point d'entrée unique) | — |
| service-clients | 8081 | Clients et garanties | Client, Collateral |
| service-credit ★ | 8082 | Dossiers de crédit et échéanciers (service principal) | LoanApplication, RepaymentSchedule |
| service-decaissements | 8083 | Versements et trésorerie | Disbursement, TreasuryAccount |
| notification-service | 8084 | Consomme RabbitMQ, envoie les emails | — |

Infra Docker : RabbitMQ (5672 / UI 15672, credigo / credigo123), MailHog (SMTP 1025 / UI 8025).

## 4. Mapping grille d'évaluation → preuves (10 points, 1 par ligne)

| Phase | Critère de la grille | Ce qui existe | Preuve (fichier dans `presentation/`) |
|---|---|---|---|
| V0 | Schéma d'architecture globale | `docs/architecture.png` | `screenshots/V0-architecture.png` |
| V0 | Diagramme de classes par microservice + gestion des références distantes | `docs/diagramme-classes.png` (références par `clientId` / `loanApplicationId`, pas d'association JPA inter-services) | `screenshots/V0-diagramme-classes.png` |
| V0 | README exhaustif | `README.md` (prérequis, schéma, ordre de lancement, endpoints) | `screenshots/V0-readme-github.png` |
| V1 | Architecture en couches, Database per Service, DTO + validation | Packages `entite / repository / service / controller / dto / mapper`, 3 bases H2 (`clientsdb`, `creditdb`, `decaissementsdb`), `@Valid` sur les DTO | `screenshots/V1-h2-console.png`, `json/V6-erreur-validation-400.txt` |
| V1 | CRUD validé par collection Postman | `postman/Credi-Go.postman_collection.json` : 91 requêtes, 147 assertions, 0 échec | `rapport-newman.html`, `screenshots/V1-rapport-newman.png` |
| V2 | Service Discovery (Eureka, statut UP) | 6 instances UP avec noms cohérents | `screenshots/V2-eureka-dashboard.png`, `json/V2-eureka-apps.json` |
| V2 | Gateway : routage dynamique + statique (lb://) | Profil par défaut = dynamique (`/service-clients/clients`), profil `statique` = routes déclarées `lb://` | `screenshots/V2-gateway-dynamique-clients.png` |
| V3 | Config Server | Services n'ont que `spring.application.name` + `spring.config.import` ; port, H2, Eureka chargés depuis `config-repo/<service>.properties` | `screenshots/V3-config-server-service-clients.png` |
| V4 | Clients OpenFeign typés + validation des références distantes avant création | `@FeignClient` ClientsClient / CreditClient / DecaissementsClient, DTOs dédiés (`ClientDistantDto`, `DossierDistantDto`...) ; dépôt refusé si client inexistant (404) ou inactif (409) | newman dossier 6 |
| V4 | Consultation enrichie (DTO composé) | `GET /credits/{id}/details` = dossier + client + garanties + échéancier + décaissements | `screenshots/V4-consultation-enrichie.png`, `json/V4-consultation-enrichie-credits-7-details.json` |
| V5 | Broker + Publisher/Consumer | `NotificationPublisher` (service-credit, service-decaissements) → file `credigo.notifications.email` → `NotificationConsumer` (`@RabbitListener`) | `json/V5-rabbitmq-queues.json`, `json/V5-notifications-consommees.json` |
| V5 | Cycle complet jusqu'au mail | Événements `LOAN_APPROVED` et `DISBURSEMENT_PAID`, emails reçus dans MailHog | `screenshots/V5-mailhog-inbox.png`, `json/V5-emails-mailhog.json` |
| V6 | Circuit Breaker, Retry, timeout sur ≥ 2 appels Feign | `ResilientRequests` : `@Retry` + `@CircuitBreaker` sur tous les appels, timeouts 1 s / 2 s | `screenshots/V6-actuator-circuitbreakers.png`, `json/V6-resilience-demo.txt` |
| V6 | Gestion centralisée des erreurs | `@RestControllerAdvice GlobalExceptionHandler` dans chaque service (400/404/409/503 avec corps JSON uniforme) | `json/V6-erreur-validation-400.txt` |
| V6 | Idempotence des créations | En-tête `Idempotency-Key` sur `POST /credits` et `POST /decaissements` : rejeu → 200 + même ressource, pas de doublon | `json/V6-idempotence-1er-appel-201.txt`, `json/V6-idempotence-rejeu-200.txt` |

## 5. Résultats des tests (exécution du 23/11/2026)

| Indicateur | Valeur |
|---|---|
| Requêtes Postman exécutées | 91 |
| Assertions | 147 |
| Échecs | 0 |
| Durée | 5,1 s |

Dossiers exécutés : 0 (infra), 1-3 (CRUD direct), 4 (gateway dynamique), 6 (flux V4 OpenFeign), 7 (notifications V5).
Dossier 5 (gateway statique) nécessite de relancer la gateway avec `--spring.profiles.active=statique`.
Dossier 8 (résilience) se lance manuellement en arrêtant un service (démo faite, voir `json/V6-resilience-demo.txt`).

## 6. Script de démo (ordre conseillé, ~5 min)

1. **Eureka** http://localhost:8761 → 6 instances UP.
2. **Config Server** http://localhost:8888/service-clients/default → montrer que le port 8081 vient du config-repo.
3. **Gateway** http://localhost:8080/service-clients/clients → routage dynamique.
4. **Postman** : lancer la collection avec le Collection Runner (ou montrer `rapport-newman.html`).
5. **Flux métier V4** (dans Postman, dossier 6) : dépôt → étude (score via Feign) → décision ACCORDE → versement → dossier DEBLOQUE + échéancier A_PAYER → `GET /credits/{id}/details`.
6. **V5** : ouvrir MailHog http://localhost:8025 → 2 emails (accord + versement) ; RabbitMQ http://localhost:15672 → file `credigo.notifications.email`, 1 consumer.
7. **V6** : arrêter service-clients → `POST /credits` renvoie 503 après 3 tentatives ; `GET /credits/7/details` renvoie 200 avec `client: null` (réponse partielle) ; http://localhost:8082/actuator/circuitbreakers → state OPEN. Rejouer un POST avec la même `Idempotency-Key` → 200 sans doublon.

## 7. Points clés à savoir expliquer (questions probables)

- **Pourquoi pas de clé étrangère entre services ?** Database per Service : chaque service a sa base ; les références distantes sont de simples identifiants, vérifiées par OpenFeign avant écriture.
- **Différence routage dynamique / statique** : dynamique = `discovery.locator.enabled=true`, une route par service Eureka (`/service-credit/credits`) ; statique = routes déclarées avec `lb://service-credit` et load balancing côté client.
- **Pourquoi le Config Server ?** Externaliser la config (ports, bases, broker) hors des jars ; les services ne gardent que leur nom et l'URL du config server.
- **Retry vs Circuit Breaker** : Retry rejoue 3 fois sur erreur transitoire ; le Circuit Breaker s'ouvre à 50 % d'échecs sur 6 appels et rejette immédiatement (fail-fast) pendant 10 s, puis half-open.
- **Idempotence** : une création rejouée avec la même clé renvoie la ressource déjà créée (200) au lieu d'en créer une deuxième.
- **Pourquoi publier après le commit ?** Pour ne pas notifier le client d'un accord qui aurait été annulé par un rollback.
- **Règles métier** : taux selon score (≥750 → 5,5 %, ≥650 → 6,5 %, ≥550 → 7,5 %, sinon refus), mensualité ≤ 40 % des revenus, garanties ≥ 30 % au-delà de 100 000 MAD, versement unique, échéancier activé seulement après versement.

## 8. Lancer / arrêter la plateforme

```bash
./start-all.sh
```

```bash
./stop-all.sh
```

Attendre environ 30 secondes après le démarrage avant la première requête Feign (cache du load balancer Spring Cloud), sinon on obtient des 503 et le circuit breaker s'ouvre.
