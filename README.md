# Credi-Go

Projet de fin de module réalisé dans le cadre de la formation **J2EE Avancée et Spring Boot** (5ème année IDSI, EMSI), enseignée par **M. KHALIL Salim**.

**Credi-Go** est une plateforme d'octroi de **crédits à la consommation** en architecture micro-services. Un client dépose une demande de crédit, son dossier est **scoré** puis **étudié** ; la décision d'accord déclenche la préparation du **décaissement** (vérification et réservation de la trésorerie), et le versement effectif **active l'échéancier de remboursement**.

**Unité de traitement : le Dossier de crédit** (`LoanApplication`) — un demandeur, un montant, une durée, un taux, un statut :

```
DEPOSE ──► EN_ETUDE ──► ACCORDE ──► DEBLOQUE
                 └──────────► REFUSE
```

Stack : Java 17 · Spring Boot 3.5.6 · Spring Cloud 2025.0.0 · Maven multi-modules · bases H2 en mémoire (une par service).

## Services

| Application | Port | Responsabilité | Entités |
|---|---|---|---|
| `service-registry` | 8761 | Annuaire Eureka : enregistrement et découverte des services | — |
| `config-server` | 8888 | Configuration centralisée (dossier `config-repo/`) | — |
| `gateway-service` | 8080 | API Gateway (Spring Cloud Gateway réactive), point d'entrée unique | — |
| `service-clients` | 8081 | Clients demandeurs de crédit et garanties apportées | `Client`, `Collateral` |
| `service-credit` ★ | 8082 | Dossiers de crédit et échéanciers *(service principal)* | `LoanApplication`, `RepaymentSchedule` |
| `service-decaissements` | 8083 | Versements des crédits et trésorerie | `Disbursement`, `TreasuryAccount` |

## Diagramme de classes

![Diagramme de classes](docs/diagramme-classes.png)

Chaque service possède ses propres données. À l'intérieur d'un service, les entités sont liées localement (ex. `LoanApplication` → `RepaymentSchedule`). **Entre services, une référence est un simple identifiant** (`clientId`, `loanApplicationId`), sans association JPA : les flèches en pointillés matérialisent ces dépendances, sans préjuger du protocole de communication.

Source éditable du diagramme : [`docs/diagramme-classes.mmd`](docs/diagramme-classes.mmd) (Mermaid).

## Architecture du projet

![Architecture](docs/architecture.png)

## Prérequis

- JDK 17
- Maven 3.9+ (ou le wrapper fourni `mvnw` / `mvnw.cmd`)
- IntelliJ IDEA (recommandé) et Postman

## Lancement

Compiler tous les modules depuis la racine :

```bash
./mvnw clean package -DskipTests        # Windows : .\mvnw.cmd clean package -DskipTests
```

Démarrer les applications **dans cet ordre** (bouton Run d'IntelliJ, ou `./mvnw -pl <module> spring-boot:run`) :

| Ordre | Application | Vérification |
|---|---|---|
| 1 | `service-registry` | http://localhost:8761 |
| 2 | `config-server` | http://localhost:8888/service-clients/default |
| 3 | `service-clients`, `service-credit`, `service-decaissements` | statut UP dans Eureka |
| 4 | `gateway-service` | http://localhost:8080/service-clients/clients |

Les services métier ne contiennent que `spring.application.name` et `spring.config.import` : port, base H2 et URL d'Eureka sont **chargés depuis le Config Server** (`config-repo/<nom-du-service>.properties` + `config-repo/application.properties` commun).

**Config Server** : profil `native` par défaut (lecture du dossier local `config-repo/`). Profil `git` disponible (`application-git.properties`) pour lire `config-repo/` depuis ce dépôt GitHub, comme dans le TP.

**Gateway** :
- sans profil → **routage dynamique** via Eureka : `http://localhost:8080/service-credit/credits`
- profil `statique` → **routes déclarées** avec répartition de charge `lb://` : `http://localhost:8080/credits`

Console H2 de chaque service : `http://localhost:808x/h2-console` (JDBC URL `jdbc:h2:mem:clientsdb`, `creditdb` ou `decaissementsdb`, utilisateur `sa`).

## Endpoints

| Service | Méthode et chemin | Description |
|---|---|---|
| clients | `GET/POST /clients`, `GET/PUT/DELETE /clients/{id}` | CRUD des clients (suppression refusée s'il a des garanties) |
| clients | `POST /clients/{id}/activation`, `/desactivation` | Changement de statut par opération dédiée |
| clients | `GET /clients/{id}/garanties` | Garanties d'un client |
| clients | `GET/POST /garanties`, `GET/PUT/DELETE /garanties/{id}` | CRUD des garanties (client existant et actif) |
| credit | `GET /credits[?clientId=&statut=]`, `POST /credits` | Liste et dépôt d'une demande (statut `DEPOSE`) |
| credit | `GET/PUT/DELETE /credits/{id}` | Modification si `DEPOSE`, suppression si `DEPOSE` ou `REFUSE` |
| credit | `GET /credits/{id}/echeancier` | Échéancier d'un dossier |
| decaissements | `GET/POST /tresoreries`, `GET/PUT/DELETE /tresoreries/{id}` | CRUD des comptes (seul le libellé est modifiable par PUT) |
| decaissements | `POST /tresoreries/{id}/approvisionnement` | Augmente le solde disponible |
| decaissements | `GET /decaissements[?dossierId=]`, `GET /decaissements/{id}` | Consultation des versements |

Codes de retour : `200`, `201` (création + en-tête `Location`), `204` (suppression), `400` (validation, avec le détail des champs), `404` (ressource inexistante), `409` (règle métier).

## Tests Postman

Importer `postman/Credi-Go.postman_collection.json`. Chaque requête contient un test automatique du code HTTP attendu ; la collection complète peut être exécutée avec le **Collection Runner** (relancer les services avant pour repartir des données de `data.sql`).

| Dossier | Contenu |
|---|---|
| 0 - Infrastructure | Services UP dans Eureka, configurations servies par le Config Server |
| 1 à 3 - Services (accès direct) | CRUD : cas valides, erreurs de validation (400), ressources inexistantes (404), règles métier (409) |
| 4 - Gateway dynamique | Mêmes ressources via `/{nom-du-service}/...` |
| 5 - Gateway statique | Routes déclarées (lancer la gateway avec le profil `statique`) |

## Règles métier principales

- Un dossier n'est étudié qu'une fois le **score du client** connu ; la décision (`ACCORDE` / `REFUSE`) fixe `montantAccorde`, `tauxPropose` et `motif`.
- Un décaissement n'est possible que pour un dossier `ACCORDE`, d'un client actif, dans la limite du **solde disponible** de la trésorerie, qui est réservé puis débité.
- Le montant accordé n'est **versé qu'une seule fois** ; en cas d'échec, les fonds réservés et non débités sont libérés et le dossier reste `ACCORDE` (décaissement à reprendre).
- L'**échéancier** n'est activé qu'après le **versement effectif** (dossier `DEBLOQUE`).

## Feuille de route des jalons

| Jalon | Contenu pour Credi-Go |
|---|---|
| Init | Description, diagramme de classes, architecture (ce README) |
| V1 | CRUD : `Client`, `Collateral` (service-clients) ; `TreasuryAccount` (service-decaissements) |
| V2 | Eureka (`service-registry`) + API Gateway (routage dynamique et statique) |
| V3 | Config Server : configuration centralisée |
| V4 | OpenFeign : CRUD et opérations métier de `LoanApplication`, `RepaymentSchedule`, `Disbursement` ; vérification du client, de son score et de ses garanties, puis de l'accord du dossier avant décaissement ; endpoint de consultation d'un dossier enrichi (client, garanties, échéancier, décaissement) |
| V5 | RabbitMQ : `service-notification` notifie le client de l'accord de son crédit et du versement des fonds |
| V6 | Résilience (Resilience4J), gestion globale des erreurs, idempotence, rate limiting de la Gateway |
| V7 | Kafka : `application-deposited`, `client-scored`, `loan-approved`, `disbursement-paid`, `disbursement-failed` ; notifications migrées de RabbitMQ vers Kafka |

Bonus envisagé : job batch mensuel de traitement des échéances (relances des retardataires, calcul des intérêts) et tableau d'amortissement côté Angular.
