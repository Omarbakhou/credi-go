#!/bin/zsh
# Relance toute la plateforme Credi-Go (à lancer depuis le dossier JEE_PROJECT)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd "$(dirname "$0")" || exit 1
mkdir -p logs
[ -f service-registry/target/service-registry-0.0.1-SNAPSHOT.jar ] || mvn -q clean package -DskipTests
docker compose up -d
pkill -f 'credi-go/.*-0.0.1-SNAPSHOT.jar' 2>/dev/null; sleep 2
run() { nohup $JAVA_HOME/bin/java -jar $1/target/$1-0.0.1-SNAPSHOT.jar > logs/$1.log 2>&1 & }
run service-registry; run config-server
echo "Attente Eureka + Config Server..."
until curl -s -o /dev/null localhost:8888/service-clients/default && curl -s -o /dev/null localhost:8761; do sleep 2; done
for s in service-clients service-credit service-decaissements notification-service gateway-service; do run $s; done
echo "Attente des services..."
until [ "$(curl -s -H 'Accept: application/json' localhost:8761/eureka/apps | grep -o '"status":"UP"' | wc -l | tr -d ' ')" -ge 6 ]; do sleep 2; done
echo "Tous les services sont UP. Patienter ~30 s avant la 1ère requête Feign (cache du load balancer)."
echo "Eureka: http://localhost:8761 | Gateway: http://localhost:8080 | MailHog: http://localhost:8025 | RabbitMQ: http://localhost:15672 (credigo/credigo123)"
