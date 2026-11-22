# Credi-Go

Projet de fin de module réalisé dans le cadre de la formation **J2EE Avancée et Spring Boot** (5ème année IDSI, EMSI), enseignée par **M. KHALIL Salim**.

**Credi-Go** est une plateforme d'octroi de **crédits à la consommation** en architecture micro-services. Un client dépose une demande de crédit, son dossier est **scoré** puis **étudié** ; la décision d'accord déclenche la préparation du **décaissement** (vérification et réservation de la trésorerie), et le versement effectif **active l'échéancier de remboursement**.

**Unité de traitement : le Dossier de crédit** (`LoanApplication`) — un demandeur, un montant, une durée, un taux, un statut :

```
DEPOSE ──► EN_ETUDE ──► ACCORDE ──► DEBLOQUE
  │              └──────────► REFUSE
  └──(annulation ou délai d'étude dépassé)──► ANNULE        (V7)
```

Stack : Java 17 · Spring Boot 3.5.6 · Spring Cloud 2025.0.0 (Eureka, Gateway, Config) · **Apache Kafka 3.9 (mode KRaft)** · Maven multi-modules · bases H2 en mémoire (une par service).

> **Branches.** `main` (tag `v6.0`) contient les jalons V1 à V6 : appels synchrones OpenFeign, notifications RabbitMQ, Resilience4j. La branche **`v7-kafka`** (tag `v7.0`) réimplémente les processus inter-services **par événements Kafka** : OpenFeign, Resilience4j et RabbitMQ y sont retirés. Ce README décrit la branche `v7-kafka`.

## Services

| Application | Port | Responsabilité | Entités |
|---|---|---|---|
| `service-registry` | 8761 | Annuaire Eureka : enregistrement et découverte des services | — |
| `config-server` | 8888 | Configuration centralisée (dossier `config-repo/`) | — |
| `gateway-service` | 8080 | API Gateway (Spring Cloud Gateway réactive), point d'entrée unique | — |
| `service-clients` | 8081 | Clients demandeurs de crédit, garanties, **scoring** ; source de `client-events` | `Client`, `Collateral` |
| `service-credit` ★ | 8082 | Dossiers de crédit et échéanciers *(service principal)* ; projections clients/garanties/décaissements | `LoanApplication`, `RepaymentSchedule` |
| `service-decaissements` | 8083 | Réservation des fonds, versements, trésorerie ; projection clients | `Disbursement`, `TreasuryAccount` |
| `notification-service` | 8084 | Consomme `loan-approved` et `disbursement-paid` et envoie les emails (seul service qui notifie) | `Notification` |
| Kafka *(Docker, KRaft)* | 9092 | Broker d'événements, sans Zookeeper | — |
| Kafka UI *(Docker)* | 8090 | Interface web : topics, messages, consumer groups | — |
| MailHog *(Docker)* | 1025 / 8025 | Serveur SMTP de test et boîte de réception web | — |

Chaque service métier possède aussi ses tables techniques : `outbox_event` (événements à publier) et `evenement_traite` (eventIds déjà consommés).

## Diagramme de classes

![Diagramme de classes](docs/diagramme-classes.png)

Chaque service possède ses propres données. À l'intérieur d'un service, les entités sont liées localement (ex. `LoanApplication` → `RepaymentSchedule`). **Entre services, une référence est un simple identifiant** (`clientId`, `loanApplicationId`), sans association JPA : les flèches en pointillés matérialisent ces dépendances, sans préjuger du protocole de communication.

Source éditable du diagramme : [`docs/diagramme-classes.mmd`](docs/diagramme-classes.mmd) (Mermaid).

## Architecture du projet

![Architecture](docs/architecture.png)

Source du schéma : [`docs/src/architecture.py`](docs/src/architecture.py) (`cd docs && py src/architecture.py`, nécessite matplotlib).

## Prérequis

- JDK 17
- Maven 3.9+ (ou le wrapper fourni `mvnw` / `mvnw.cmd`)
- IntelliJ IDEA (recommandé) et Postman (ou `npx newman`)
- Docker Desktop (Kafka, Kafka UI et MailHog)

## Lancement

Compiler tous les modules depuis la racine :

```bash
./mvnw clean package -DskipTests        # Windows : .\mvnw.cmd clean package -DskipTests
```

Démarrer Kafka (KRaft), Kafka UI et MailHog :

```bash
docker compose up -d
```

Puis démarrer les applications **dans cet ordre** (bouton Run d'IntelliJ, ou `./mvnw -pl <module> spring-boot:run`) :

| Ordre | Application | Vérification |
|---|---|---|
| 1 | `service-registry` | http://localhost:8761 |
| 2 | `config-server` | http://localhost:8888/service-clients/default |
| 3 | `service-clients`, `service-credit`, `service-decaissements`, `notification-service` | statut UP dans Eureka |
| 4 | `gateway-service` | http://localhost:8080/service-clients/clients |

Les topics sont créés au démarrage (3 partitions, `client-events` compacté). Au démarrage, `service-clients` republie l'état de tous ses clients (`CLIENT_SNAPSHOT`) et les projections de service-credit et service-decaissements relisent `client-events` depuis le début : les références distantes sont donc disponibles sans appel synchrone.

Les services métier ne contiennent que `spring.application.name` et `spring.config.import` : port, base H2, URL d'Eureka et paramètres Kafka sont **chargés depuis le Config Server** (`config-repo/<nom-du-service>.properties` + `config-repo/application.properties` commun).

**Config Server** : profil `native` par défaut (lecture du dossier local `config-repo/`). Profil `git` disponible (`application-git.properties`) pour lire `config-repo/` depuis ce dépôt GitHub, comme dans le TP.

**Gateway** :
- sans profil → **routage dynamique** via Eureka : `http://localhost:8080/service-credit/credits`
- profil `statique` → **routes déclarées** avec répartition de charge `lb://` : `http://localhost:8080/credits`

Console H2 de chaque service : `http://localhost:808x/h2-console` (JDBC URL `jdbc:h2:mem:clientsdb`, `creditdb`, `decaissementsdb` ou `notificationsdb`, utilisateur `sa`).

## Endpoints

| Service | Méthode et chemin | Description |
|---|---|---|
| clients | `GET/POST /clients`, `GET/PUT/DELETE /clients/{id}` | CRUD des clients (suppression refusée s'il a des garanties) ; chaque écriture publie un événement `client-events` |
| clients | `POST /clients/{id}/activation`, `/desactivation` | Changement de statut par opération dédiée |
| clients | `GET /clients/{id}/garanties` | Garanties d'un client |
| clients | `GET/POST /garanties`, `GET/PUT/DELETE /garanties/{id}` | CRUD des garanties (client existant et actif) |
| clients | `POST /evenements/clients/republication` | Republie l'état de tous les clients (initialisation des projections) |
| credit | `GET /credits[?clientId=&statut=]`, `POST /credits` | Liste et dépôt d'une demande (`DEPOSE`, publie `application-deposited`) |
| credit | `GET/PUT/DELETE /credits/{id}` | Modification si `DEPOSE` (nouvelle version, nouveau scoring), suppression si `DEPOSE`, `REFUSE` ou `ANNULE` |
| credit | `GET /credits/{id}/echeancier` | Échéancier d'un dossier |
| credit | `POST /credits/{id}/decision` | `EN_ETUDE` → `ACCORDE` (échéancier `INACTIVE`, publie `loan-approved`) ou `REFUSE` |
| credit | `POST /credits/{id}/annulation` | Annulation d'une demande `DEPOSE` ou `EN_ETUDE` → `ANNULE` |
| credit | `POST /credits/{id}/relance-decaissement` | Après un `disbursement-failed` : nouvelle tentative (`loan-approved`, tentative + 1) |
| credit | `GET /credits/{id}/details` | Consultation enrichie composée depuis les **projections locales** (client, garanties, décaissements) |
| credit, decaissements | `GET /projections/clients[/{id}]`, `POST /projections/clients/reconstruction` | Projection locale des clients ; reconstruction par relecture de `client-events` |
| decaissements | `GET/POST /tresoreries`, `GET/PUT/DELETE /tresoreries/{id}` | CRUD des comptes (seul le libellé est modifiable par PUT) |
| decaissements | `POST /tresoreries/{id}/approvisionnement` | Augmente le solde disponible |
| decaissements | `GET /decaissements[?dossierId=]`, `GET /decaissements/{id}` | Consultation des décaissements (`RESERVE`, `VERSE`, `ECHOUE`, `ANNULE`) |
| decaissements | `POST /decaissements/{id}/versement` | Versement effectif d'une réservation : débit des fonds, publie `disbursement-paid` (idempotent) |
| decaissements | `POST /decaissements/{id}/annulation` | Annulation d'une réservation non versée : fonds libérés, publie `disbursement-failed` |
| tous les services métier | `GET /evenements/outbox[?correlationId=]`, `GET /evenements/traites[?correlationId=]` | Supervision : événements publiés (outbox) et événements consommés (idempotence) |
| notification | `GET /notifications` | Journal des notifications et statut d'envoi |

`POST /credits` accepte l'en-tête **`Idempotency-Key`** (V6) : une requête rejouée avec la même clé renvoie `200` et le dossier déjà créé, sans doublon ni second événement.

Codes de retour : `200`, `201` (création + en-tête `Location`), `202` (reconstruction ou republication lancée), `204` (suppression), `400` (validation, avec le détail des champs), `404` (ressource inexistante, locale ou dans la projection), `409` (règle métier, conflit de version), `503` (donnée de référence absente de la projection).

## Tests Postman

Importer `postman/Credi-Go.postman_collection.json`. Chaque requête contient un test automatique du code HTTP attendu ; la collection peut être exécutée avec le **Collection Runner** (relancer les services avant pour repartir des données de `data.sql`).

| Dossier | Contenu |
|---|---|
| 0 - Infrastructure | Services UP dans Eureka, configurations servies par le Config Server |
| 1 à 3 - Services (accès direct) | CRUD : cas valides, erreurs de validation (400), ressources inexistantes (404), règles métier (409) |
| 4 - Gateway dynamique | Mêmes ressources via `/{nom-du-service}/...` |
| 5 - Gateway statique | Routes déclarées (lancer la gateway avec le profil `statique`) |
| 6 à 8 - V4 à V6 | OpenFeign, RabbitMQ, résilience : **branche `main`** uniquement |
| **9 - V7 : Kafka** | Flux complet par événements et vérification de l'état final (détails ci-dessous) |

Sur la branche `v7-kafka`, avec newman :

```bash
npx newman run postman/Credi-Go.postman_collection.json \
  --folder "0 - Infrastructure (Eureka, Config Server)" --folder "1 - service-clients (direct :8081)" \
  --folder "2 - service-credit (direct :8082)" --folder "3 - service-decaissements (direct :8083)" \
  --folder "4 - Gateway :8080 - routage dynamique (profil par défaut)" --folder "9 - V7 : Kafka"
```

Le dossier 9 (50 requêtes) attend les étapes asynchrones en réinterrogeant l'API (400 ms, 40 essais max) et vérifie :
1. dépôt (`DEPOSE`) → score calculé par service-clients (`scoreInterne`) → dossier `EN_ETUDE` ;
2. décision `ACCORDE`, échéancier `INACTIVE` → réservation des fonds (`RESERVE`, solde disponible diminué) ;
3. annulation de la réservation → `disbursement-failed`, fonds libérés, dossier `ACCORDE` « décaissement à reprendre », versement tardif refusé (409) ;
4. relance → nouvelle réservation → versement → dossier **`DEBLOQUE`**, échéancier **`A_PAYER`**, trésorerie **débitée une seule fois** ;
5. notifications envoyées et **emails reçus dans MailHog**, outbox et événements traités par `correlationId` ;
6. refus (score insuffisant), annulation avant scoring (score tardif ignoré), propagation création/désactivation/suppression d'un client aux projections, reconstruction d'une projection.

## Communication entre services (V7 : Kafka)

Les processus qui traversent plusieurs services sont **asynchrones** : aucun appel HTTP direct entre services métier.

| Topic | Producteur → consommateur(s) | Traitement |
|---|---|---|
| `application-deposited` | service-credit → service-clients | Calcule le score du client pour cette demande (règle sur les revenus mensuels) et l'enregistre dans `scoreInterne` |
| `client-scored` | service-clients → service-credit | Dossier `DEPOSE` → `EN_ETUDE` avec le score, avant la décision ; client inéligible → `REFUSE` |
| `loan-approved` | service-credit → service-decaissements | Prépare le décaissement du `montantAccorde` et **réserve les fonds de façon atomique** (`UPDATE` conditionnel sur le solde) ; l'échéancier reste `INACTIVE` |
| `loan-approved` | service-credit → notification-service | Email d'accord au client |
| `disbursement-paid` | service-decaissements → service-credit | Dossier → `DEBLOQUE`, échéancier activé (`A_PAYER`) |
| `disbursement-paid` | service-decaissements → notification-service | Email de confirmation du versement |
| `disbursement-failed` | service-decaissements → service-credit | Dossier maintenu `ACCORDE`, décaissement à reprendre, échéancier inactif ; les fonds réservés non débités sont libérés |
| `client-events` *(compacté)* | service-clients → service-credit, service-decaissements | `CLIENT_CREATED`, `_UPDATED`, `_ACTIVATED`, `_DEACTIVATED`, `_DELETED`, `_SNAPSHOT` : alimentent les **projections locales** (clients et garanties) |

**Enveloppe commune** (`EvenementMetier`, JSON, clé Kafka = identifiant métier) : `eventId`, `eventType`, `version` (de l'agrégat), `correlationId` (identique sur tout le cycle du dossier), `causationId`, `businessId`, `source`, `occurredAt`, `data` (données nécessaires au traitement : montant accordé, email du client, motif d'échec…).

**Publication fiable — transactional outbox** : l'événement est écrit dans la table `outbox_event` **dans la même transaction** que le changement d'état. Un publieur planifié (`OutboxPublisher`, toutes les 300 ms) n'envoie que des lignes validées, avec `acks=all` et producteur idempotent ; il ne les marque `PUBLIE` qu'après l'acquittement du broker. Kafka arrêté : la ligne reste `EN_ATTENTE` (nombre d'essais et dernière erreur conservés) et repart au passage suivant, dans l'ordre d'émission.

**Consommateurs idempotents** : `ConsommateurIdempotent` vérifie l'`eventId` dans `evenement_traite`, applique le traitement et enregistre l'`eventId` **dans une seule transaction** ; un doublon est ignoré. En cas d'erreur : 3 essais espacés d'1 s, puis lettre morte dans `<topic>.DLT` ; un message illisible part directement en `.DLT`.

**Refus, délais et compensations** :
- client inconnu ou inactif dans la projection → dépôt refusé (404 / 409) ; client inéligible ou score insuffisant → `REFUSE` ;
- **timeout du scoring** : une demande restée `DEPOSE` plus de 2 min passe `ANNULE` (`ExpirationEtudes`) ;
- **timeout du versement** : une réservation non versée sous 10 min expire (`ExpirationReservations`) → fonds libérés, `disbursement-failed` ;
- trésorerie insuffisante, client devenu inactif, annulation par l'opérateur → `ECHOUE` / `ANNULE`, fonds libérés, `disbursement-failed` ; `POST /credits/{id}/relance-decaissement` relance une nouvelle tentative ;
- **un résultat tardif ne réactive jamais une opération annulée** : un `client-scored` sur un dossier qui n'est plus `DEPOSE`, ou portant une ancienne `version` de la demande, est ignoré (et tracé) ; un décaissement `ANNULE`/`ECHOUE` ne peut plus être versé ; un échec tardif n'écrase pas un versement effectué.

**Projections et données de référence** : service-credit (clients, garanties, décaissements) et service-decaissements (clients) décident **sur leurs propres données** à partir de projections locales. Initialisation : republication de l'état courant par service-clients au démarrage, relecture de `client-events` depuis le début à chaque démarrage des consommateurs ; reconstruction à la demande par `POST /projections/clients/reconstruction`.

**Conservé de V6** : gestion globale des erreurs (`@RestControllerAdvice`, avec en plus `409` sur conflit de version optimiste) et idempotence du dépôt par `Idempotency-Key`.

Supervision : Kafka UI (http://localhost:8090) pour les topics, messages et consumer groups ; `GET /evenements/outbox` et `/evenements/traites?correlationId=…` sur chaque service ; MailHog (http://localhost:8025) pour les emails.

## Règles métier principales

- **Scoring** (service-clients) selon les revenus mensuels : ≥ 15 000 MAD → 800 ; ≥ 10 000 → 720 ; ≥ 8 000 → 650 ; ≥ 5 000 → 560 ; sinon 480 ; −50 si le montant demandé dépasse 30 fois les revenus (minimum 300).
- Un dossier n'est étudié qu'une fois le **score du client** connu. La décision (`ACCORDE` / `REFUSE`) fixe `montantAccorde`, `tauxPropose` et `commentaireDecision` :
  - taux selon le score : ≥ 750 → 5,50 % ; ≥ 650 → 6,50 % ; ≥ 550 → 7,50 % ; en dessous → refus ;
  - la 1ère mensualité ne dépasse pas **40 % des revenus** (sinon accord partiel, ou refus sous 50 % du montant demandé) ;
  - au-delà de 100 000 MAD, les garanties doivent couvrir au moins 30 % du montant.
- Un décaissement n'est possible que pour un dossier `ACCORDE`, d'un client actif, dans la limite du **solde disponible** de la trésorerie : les fonds sont **réservés** à l'accord puis **débités** au versement.
- Le montant accordé n'est **versé qu'une seule fois** ; en cas d'échec, les fonds réservés sont libérés et le dossier reste `ACCORDE` (décaissement à reprendre).
- L'**échéancier** n'est activé qu'après le **versement effectif** (dossier `DEBLOQUE`).

## Feuille de route des jalons

| Jalon | Contenu pour Credi-Go | Tag |
|---|---|---|
| Init | Description, diagramme de classes, architecture (ce README) | — |
| V1 | CRUD : `Client`, `Collateral` (service-clients) ; `TreasuryAccount` (service-decaissements) | `v1.0` |
| V2 | Eureka (`service-registry`) + API Gateway (routage dynamique et statique) | `v2.0` |
| V3 | Config Server : configuration centralisée | `v3.0` |
| V4 | OpenFeign : étude, décision, échéancier, versement et déblocage ; vérification des références distantes avant écriture ; consultation enrichie d'un dossier | `v4.0` |
| V5 | RabbitMQ : `notification-service` notifie le client de l'accord de son crédit et du versement des fonds (MailHog) | `v5.0` |
| V6 | Résilience (Resilience4j : Retry, Circuit Breaker, timeouts), gestion globale des erreurs, idempotence des créations | `v6.0` |
| V7 | Kafka (KRaft) : `application-deposited`, `client-scored`, `loan-approved`, `disbursement-paid`, `disbursement-failed`, `client-events` ; outbox transactionnelle, consommateurs idempotents, compensations, projections locales ; notifications migrées de RabbitMQ vers Kafka (branche `v7-kafka`) | `v7.0` |

Bonus envisagé : job batch mensuel de traitement des échéances (relances des retardataires, calcul des intérêts) et tableau d'amortissement côté Angular.
