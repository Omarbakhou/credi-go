# Déroulé de soutenance — quoi faire sur la machine, ligne par ligne de la grille

## Avant d'aller le voir (2 minutes)

1. Terminal, dans `JEE_PROJECT` : `./start-all.sh` → attendre la ligne **PRÊT**.
2. Onglets navigateur ouverts : Eureka (8761), Config Server (8888/service-clients/default), Gateway (8080/service-clients/clients), MailHog (8025), GitHub (Omarbakhou/credi-go).
3. Postman ouvert sur la collection « Credi-Go - V1 à V6 » (dossiers 0 à 7), rien lancé.
4. IntelliJ ouvert sur le projet, arbre replié.
5. Slides ouvertes (Credi-Go-Presentation.pptx), slide 1.

Phrase d'ouverture : « Credi-Go, octroi de crédits à la consommation en micro-services. Sept applications Spring Boot, une base par service, Eureka, Gateway, Config Server, OpenFeign, RabbitMQ, Resilience4j. »

---

## V0 — Conception & documentation (slides 4, 5, 6)

| Il demande | Tu montres | Tu dis |
|---|---|---|
| Schéma d'architecture | Slide 4 (ou onglet GitHub, le README affiche l'image) | « Gateway point d'entrée, Eureka pour la découverte, Config Server, 3 services métier avec chacun sa base H2, RabbitMQ vers notification-service. » |
| Diagramme de classes / références distantes | Slide 5 | « Entre services, une référence est un identifiant : `clientId`, `loanApplicationId`. Pas d'association JPA, c'est OpenFeign qui vérifie. » |
| README | Onglet GitHub, scroller | « Prérequis, ordre de lancement, endpoints, règles métier, feuille de route. » |

## V1 — Métier & modèle de données (slides 7, 8)

| Il demande | Tu montres | Tu dis |
|---|---|---|
| Architecture en couches | IntelliJ : `service-clients/src/main/java/com/credigo/clients` → ouvrir `controller/ClientController`, `service/ClientService`, `repository/ClientRepository`, `entite/Client` | « Controller, Service, Repository, Entity, plus `dto` et `mapper`. Même structure dans les 3 services. » |
| Database per service | IntelliJ : `config-repo/service-clients.properties`, `service-credit.properties`, `service-decaissements.properties` | « Trois bases H2 : clientsdb, creditdb, decaissementsdb. » |
| Validation DTO | IntelliJ : `service-clients/.../dto/ClientDto.java` (`@NotBlank`, `@Email`), puis `ClientController` ligne `@Valid @RequestBody` | « Bean Validation sur le DTO, le mapper convertit en entité, 400 avec le détail des champs. » |
| CRUD validé par Postman | Postman : dossier **1 - service-clients** → **Créer un client invalide -> 400** → **Send** → onglet **Test Results** en bas | « Chaque requête teste son code HTTP. Cas valides, 400, 404, 409. » |

## V2 & V3 — Eureka, Gateway, Config Server (slides 9, 10, 11)

| Il demande | Tu montres | Tu dis |
|---|---|---|
| Service Discovery | Onglet **Eureka** : 6 instances UP | « Chaque service porte `@EnableDiscoveryClient` et s'enregistre sous son `spring.application.name`. » Code : `service-registry/.../ServiceRegistryApplication.java` (`@EnableEurekaServer`). |
| Routage dynamique | Onglet **Gateway** `localhost:8080/service-clients/clients` | « Le préfixe est le nom du service dans Eureka. » Code : `config-repo/gateway-service.properties` → `discovery.locator.enabled=true`. |
| Routage statique / load balancing | IntelliJ : `config-repo/gateway-service-statique.properties` | « Profil `statique` : routes déclarées, `lb://service-credit` délègue au load balancer. Validé par la collection Manuels, dossier 5. » |
| Config Server | Onglet **Config Server** `localhost:8888/service-clients/default` → montrer `server.port: 8081`. Puis IntelliJ : `service-clients/src/main/resources/application.properties` (2 lignes) | « Le jar ne connaît que son nom et l'URL du Config Server. Port, base, Eureka, RabbitMQ viennent du dossier config-repo. » |

## V4 — OpenFeign (slide 12)

Dans Postman, dossier **6 - V4 : flux du crédit**, **une requête à la fois, de haut en bas, un seul Send chacune** :

| Requête | Résultat à montrer | Tu dis |
|---|---|---|
| Dépôt pour un client inexistant -> 404 | 404 « Client 9999 introuvable dans service-clients » | « service-credit a interrogé service-clients par Feign avant d'écrire. » |
| Dépôt pour un client inactif -> 409 | 409 | « Règle métier : client inactif. » |
| Déposer une demande (client 2) -> 201 | `statut: DEPOSE` | — |
| Mettre à l'étude | `EN_ETUDE`, `scoreEtude: 650` | « Le score vient de service-clients via Feign. » |
| Décision -> ACCORDE | `tauxPropose: 6.5`, `montantAccorde: 25000` | « Taux selon le score, mensualité ≤ 40 % des revenus. » |
| Échéancier généré mais INACTIF | 24 lignes `INACTIVE` | « Pas activé tant que les fonds ne sont pas versés. » |
| Verser les fonds -> 201 VERSE | `statut: VERSE` | « service-decaissements a vérifié le dossier ACCORDE via Feign, débité la trésorerie, puis appelé le déblocage. » |
| Dossier DEBLOQUE / Échéancier activé | `DEBLOQUE`, `A_PAYER` | — |
| Consultation enrichie | `dossier`, `client`, `garanties`, `echeancier`, `decaissements`, `donneesPartielles: false` | « DTO composé, agrégé par Feign depuis deux services. » |

Code si demandé : `service-credit/.../feign/ClientsClient.java` (`@FeignClient(name = "service-clients")`), `feign/ClientDistantDto.java`, `service/CreditService.java` (`deposer`, `etudier`, `decider`, `consulterDetail`), `dto/DossierDetailDto.java`.

## V5 — RabbitMQ (slide 13)

| Il demande | Tu montres | Tu dis |
|---|---|---|
| Publisher / Consumer | IntelliJ : `service-credit/.../messaging/NotificationPublisher.java` (`afterCommit` → `convertAndSend`), `messaging/RabbitConfig.java` (file `credigo.notifications.email`), `notification-service/.../messaging/NotificationConsumer.java` (`@RabbitListener`) | « Publication après le commit pour ne jamais notifier un accord annulé. » |
| Cycle complet | Onglet **MailHog** : les deux mails « crédit accordé » et « fonds versés » du dossier que tu viens de faire. Puis `localhost:15672` (credigo / credigo123) → Queues → `credigo.notifications.email`, Consumers = 1. Puis `localhost:8084/notifications` | « Événement LOAN_APPROVED à la décision, DISBURSEMENT_PAID au versement. » |

## V6 — Résilience, erreurs, idempotence (slide 14)

| Il demande | Tu montres | Tu dis |
|---|---|---|
| Circuit Breaker / Retry / timeout | IntelliJ : `service-credit/.../feign/ResilientRequests.java` (`@Retry` + `@CircuitBreaker` sur chaque méthode, fallbacks en bas) ; `config-repo/service-credit.properties` section V6 | « Retry 3 tentatives, circuit ouvert à 50 % d'échecs sur 6 appels, timeouts 1 s / 2 s. Deux appels Feign protégés dans chaque service. » |
| Démo résilience (s'il insiste) | Terminal : `pkill -f service-clients-0.0.1`. Postman, collection **Manuels**, dossier 8 : **[service-clients ARRÊTÉ] Dépôt** → 503 après ~1,5 s ; **Consultation enrichie** → 200 avec `client: null`, `donneesPartielles: true`. Navigateur : `localhost:8082/actuator/circuitbreakers` → `OPEN`. Ensuite `./start-all.sh`. | « Repli strict en écriture, tolérant en lecture. » |
| ControllerAdvice | IntelliJ : `service-credit/.../exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`, 400/404/409/503) | « Un seul format d'erreur JSON pour tout le service. » |
| Idempotence | Postman dossier 6 : **V6 - Rejouer le même dépôt (même Idempotency-Key)** → 200 et même `id`. Code : `CreditController.creer` (`@RequestHeader("Idempotency-Key")`), `CreditService.deposer` (`findByIdempotencyKey`) | « Un rejeu renvoie la ressource déjà créée, pas de doublon. » |

## Pour finir en force

Postman : clic droit sur la collection « Credi-Go - V1 à V6 » → **Run collection** → **Run**. 92 requêtes, 148 tests, tout vert, en 5 secondes. Rejouable sans redémarrer.

## Si quelque chose casse

- 503 sur une requête Feign : `./start-all.sh`, attendre PRÊT, recommencer.
- Gateway absente d'Eureka : même chose.
- Une requête du dossier 6 renvoie 409 « seul un dossier DEPOSE peut être mis à l'étude » : tu as cliqué deux fois, reprends à « Déposer une demande ».

## Bonus — Interface Angular (si tu veux impressionner)

Lancer avant : `./start-front.sh` (depuis JEE_PROJECT) → http://localhost:4200.
Onglet **Dossiers de crédit** : formulaire « Nouvelle demande » → **Déposer** → sur la ligne créée : **Mettre à l'étude** → **Décider** → **Verser les fonds** → **Tableau d'amortissement**. Puis onglet **Notifications** (les deux mails) et **Trésorerie** (solde débité).
Tu dis : « Le front ne parle qu'à la Gateway. Le tableau d'amortissement est calculé à partir de l'échéancier du service-credit : mensualité et capital restant dû. »
