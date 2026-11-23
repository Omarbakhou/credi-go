# Guide oral — « Où est cette partie dans le code ? »

Le prof sort la fiche des jalons et demande où se trouve chaque élément dans le code, puis pose des questions. Ce guide donne, pour chaque ligne de la grille, le fichier à ouvrir dans IntelliJ et la phrase à dire.

Tous les chemins sont relatifs à `credi-go/`. Les packages Java commencent par `src/main/java/com/credigo/<service>/`.

---

## V0 — Conception & documentation

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Schéma d'architecture | `docs/architecture.png` (source : `docs/src/architecture.py`) | « 7 applications Spring Boot, 3 bases H2 isolées, RabbitMQ et MailHog sous Docker. » |
| Diagramme de classes | `docs/diagramme-classes.png` (source Mermaid : `docs/diagramme-classes.mmd`) | « Entre services, une référence est un identifiant (`clientId`, `loanApplicationId`), jamais une association JPA. » |
| README | `README.md` | « Prérequis, ordre de lancement, tableau des endpoints, règles métier, feuille de route. » |

## V1 — Implémentation métier & modèle de données

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Architecture en couches | `service-clients/.../entite/Client.java` → `repository/ClientRepository.java` → `service/ClientService.java` → `controller/ClientController.java` | « Entity, Repository, Service, Controller, plus un package `dto` et `mapper`. Même structure dans les 3 services. » |
| Database per Service | `config-repo/service-clients.properties` (`jdbc:h2:mem:clientsdb`), `service-credit.properties` (`creditdb`), `service-decaissements.properties` (`decaissementsdb`) | « Une base H2 en mémoire par service, aucun service n'accède à la base d'un autre. » |
| Validation / mapping DTO | `service-clients/.../dto/ClientDto.java` (`@NotBlank`, `@Email`, `@PositiveOrZero`, `@Min`) ; `controller/ClientController.java` ligne 43 (`@Valid @RequestBody`) ; `mapper/ClientMapper.java` | « Le DTO est validé par Bean Validation, puis le mapper le convertit en entité. Une erreur renvoie 400 avec le détail des champs. » |
| CRUD autonomes | `ClientController`, `CollateralController` (service-clients) ; `TresorerieController` (service-decaissements) | « GET/POST/PUT/DELETE + opérations dédiées : activation, désactivation, approvisionnement. » |
| Données de démo | `*/src/main/resources/data.sql` | « Chargées au démarrage grâce à `spring.sql.init.mode=always`. » |
| Collection Postman | `postman/Credi-Go.postman_collection.json` | « 91 requêtes, 147 assertions, chaque requête teste son code HTTP. Dossiers 1 à 3 = CRUD de chaque service. » |

## V2 — Eureka & Gateway

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Eureka Server | `service-registry/.../ServiceRegistryApplication.java` (`@EnableEurekaServer`) ; `service-registry/src/main/resources/application.properties` (`register-with-eureka=false`, `fetch-registry=false`) | « Le registre ne s'enregistre pas lui-même. Port 8761. » |
| Eureka Client | `service-clients/.../ServiceClientsApplication.java` (`@EnableDiscoveryClient`) ; URL Eureka dans `config-repo/application.properties` | « Chaque service s'enregistre sous son `spring.application.name`. » |
| Routage dynamique | `config-repo/gateway-service.properties` : `discovery.locator.enabled=true` | « Une route par service Eureka : `/service-clients/clients`. » |
| Routage statique + load balancing | `config-repo/gateway-service-statique.properties` : `routes[0].uri=lb://service-clients`, `Path=/clients/**` | « Profil `statique` : routes déclarées, `lb://` délègue au load balancer côté client. Postman dossier 5. » |

## V3 — Config Server

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Serveur | `config-server/.../ConfigServerApplication.java` (`@EnableConfigServer`) ; `config-server/src/main/resources/application.properties` (profil `native`, `search-locations=file:./config-repo/`) ; `application-git.properties` (profil `git`) | « Mode native lit le dossier `config-repo/` ; mode git lit le dépôt GitHub. » |
| Fichiers de config | `config-repo/application.properties` (commun), `config-repo/service-credit.properties`, etc. | « Un fichier par service, nommé comme le service, plus un fichier commun. » |
| Côté client | `service-credit/src/main/resources/application.properties` : seulement `spring.application.name` et `spring.config.import=configserver:http://localhost:8888` | « Le port 8082, la base H2 et Eureka ne sont pas dans le jar : tout vient du Config Server. Vérifiable sur http://localhost:8888/service-credit/default. » |

## V4 — OpenFeign

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Client Feign typé | `service-credit/.../feign/ClientsClient.java` (`@FeignClient(name = "service-clients")`), `feign/DecaissementsClient.java` ; `service-decaissements/.../feign/CreditClient.java`, `feign/ClientsClient.java` | « Le nom dans `@FeignClient` est le nom Eureka, pas une URL. » |
| DTO dédiés | `service-credit/.../feign/ClientDistantDto.java`, `GarantieDistanteDto.java`, `DecaissementDistantDto.java` ; `service-decaissements/.../feign/DossierDistantDto.java` | « On ne partage pas les entités entre services, chaque appelant a ses propres DTO distants. » |
| Validation avant création | `service-credit/.../service/CreditService.java` : `deposer()` ligne 98 (client existant et ACTIF), `etudier()` ligne 137 (score), `decider()` ligne 150 (revenus, garanties) ; `service-decaissements/.../service/DecaissementService.java` : `verser()` ligne 75 (dossier ACCORDE, client ACTIF) | « Chaque référence distante est vérifiée avant toute écriture. Client inexistant → 404, inactif → 409. » |
| Règles d'octroi | `service-credit/.../service/RegleOctroi.java` | « Taux selon score, mensualité ≤ 40 % des revenus, garanties ≥ 30 % au-delà de 100 000 MAD. » |
| Activation Feign | `ServiceCreditApplication.java` (`@EnableFeignClients`) | — |
| Consultation enrichie | `CreditService.consulterDetail()` ligne 82 ; `dto/DossierDetailDto.java` ; `CreditController` → `GET /credits/{id}/details` | « Un DTO composé : dossier + client + garanties + échéancier + décaissements, agrégés par Feign. Postman dossier 6. » |

## V5 — RabbitMQ & notification

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Config du broker | `service-credit/.../messaging/RabbitConfig.java` (file `credigo.notifications.email`, `Jackson2JsonMessageConverter`) ; `docker-compose.yml` ; `config-repo/application.properties` (host, user) | « File durable, messages JSON. RabbitMQ tourne dans Docker. » |
| Publisher | `service-credit/.../messaging/NotificationPublisher.java` → `publierApresCommit()` (`TransactionSynchronization.afterCommit` puis `rabbitTemplate.convertAndSend`) ; même classe dans service-decaissements | « On publie après le commit pour ne jamais notifier un accord annulé par un rollback. » |
| Événements | `CreditService.decider()` → `LOAN_APPROVED` ; `DecaissementService.verser()` → `DISBURSEMENT_PAID` | — |
| Consumer | `notification-service/.../messaging/NotificationConsumer.java` (`@RabbitListener(queues = RabbitConfig.QUEUE_EMAIL)`) → `service/NotificationService.java` (envoi SMTP via `JavaMailSender`) ; `controller/NotificationController.java` (`GET /notifications`) | « Le consumer envoie l'email à MailHog et garde l'historique. Démo : MailHog http://localhost:8025. » |

## V6 — Résilience, erreurs, idempotence

| Demande du prof | Fichier à ouvrir | Phrase à dire |
|---|---|---|
| Circuit Breaker + Retry | `service-credit/.../feign/ResilientRequests.java` : `@Retry(name="service-clients", fallbackMethod=...)` + `@CircuitBreaker(name="service-clients")` lignes 43-71 ; fallbacks lignes 77-101 ; même classe dans service-decaissements | « Tous les appels Feign passent par cette classe. Les appels `Strict` refusent (503), les appels `Tolerant` renvoient une valeur de repli. » |
| Paramètres | `config-repo/service-credit.properties` section V6 : `max-attempts=3`, `sliding-window-size=6`, `failure-rate-threshold=50`, `wait-duration-in-open-state=10s` | « Retry englobe le Circuit Breaker (`retry-aspect-order=1`). Les erreurs 4xx ne comptent pas comme échec. » |
| Timeout | `config-repo/service-credit.properties` : `openfeign.client.config.default.connectTimeout=1000`, `readTimeout=2000` | — |
| État du circuit | http://localhost:8082/actuator/circuitbreakers | « CLOSED → OPEN à 50 % d'échecs → HALF_OPEN après 10 s. » |
| ControllerAdvice | `service-credit/.../exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`) : 404 `RessourceIntrouvable`, 409 `RegleMetier`, 503 `ServiceExterneIndisponible`, 400 validation ; `exception/ErreurResponse.java` | « Un seul format d'erreur JSON pour tout le service : timestamp, status, erreur, message, chemin, details. » |
| Idempotence | `CreditController.creer()` (`@RequestHeader("Idempotency-Key")`) → `CreditService.deposer()` ligne 98 (`findByIdempotencyKey`) ; `repository/LoanApplicationRepository.java` ligne 16 ; colonne `idempotencyKey` dans `entite/LoanApplication.java` ; pareil pour `POST /decaissements` | « Un rejeu avec la même clé renvoie 200 et le dossier déjà créé, pas de doublon. Postman dossier 6 : "Rejouer le même dépôt". » |

---

## Tests Postman — ce qu'il faut savoir montrer

- **Fichier** : `postman/Credi-Go.postman_collection.json`, à importer dans Postman (File → Import).
- **Structure** : 9 dossiers numérotés 0 à 8, un par jalon. Chaque requête a un onglet *Tests* avec `pm.test("Statut HTTP 201", ...)`.
- **Variables** : les requêtes de création enregistrent l'id créé (`pm.collectionVariables.set(...)`) pour les requêtes suivantes (modifier, supprimer).
- **Collection Runner** : clic droit sur la collection → *Run collection* → tout s'exécute dans l'ordre. Relancer les services avant pour repartir des données de `data.sql`.
- **Résultat du 23/11** : 91 requêtes, 147 assertions, 0 échec (rapport : `presentation/rapport-newman.html`).
- **Dossier 5** (gateway statique) : lancer la gateway avec `--spring.profiles.active=statique`.
- **Dossier 8** (résilience) : à lancer à la main après avoir arrêté service-clients.
- Si on demande « pourquoi un 409 et pas un 400 ? » : 400 = données invalides (format), 409 = données valides mais règle métier violée (client inactif, dossier pas au bon statut, solde insuffisant).

## Questions probables et réponses courtes

- **Pourquoi H2 et pas MySQL ?** Base en mémoire, zéro installation, recréée à chaque démarrage avec `data.sql`. Le principe Database per Service reste identique avec MySQL : il suffit de changer l'URL dans le config-repo.
- **Comment la Gateway trouve-t-elle les services ?** Par Eureka : elle a `@EnableDiscoveryClient` et le locator crée une route par service enregistré.
- **Que se passe-t-il si Eureka tombe ?** Les clients gardent le dernier registre en cache ; les nouveaux services ne pourront plus s'enregistrer.
- **Pourquoi Feign et pas RestTemplate ?** Interface déclarative, intégrée à Eureka et au load balancer, les DTO sont typés.
- **Différence Retry / Circuit Breaker ?** Retry rejoue l'appel ; le Circuit Breaker arrête d'appeler un service en panne (fail-fast) pour ne pas aggraver la situation.
- **Pourquoi publier après le commit ?** Si la transaction est annulée, le message ne doit pas partir.
- **Pourquoi l'idempotence ?** Un client qui rejoue une requête (timeout réseau, double clic) ne doit pas créer deux dossiers ni deux versements.
- **Transaction locale au versement** : dans `DecaissementService.verser()` (`@Transactional`), on réserve et débite la trésorerie, puis on appelle le déblocage côté service-credit ; si cet appel échoue, l'exception annule la transaction et les fonds sont libérés.
- **Ce qui manque** : V7 (Kafka) non réalisé, prévu en branche à partir de V3.

## Ordre de lancement (IntelliJ ou script)

1. `docker compose up -d` (RabbitMQ, MailHog)
2. service-registry → config-server
3. service-clients, service-credit, service-decaissements, notification-service
4. gateway-service
5. Attendre 30 s avant la première requête Feign (cache du load balancer), sinon 503 et circuit ouvert.

Script : `../start-all.sh` depuis le dossier `JEE_PROJECT`.
