#!/bin/zsh
# Démo équilibrage de charge : lance une 2e instance de service-clients (port 8091).
# Eureka affiche alors SERVICE-CLIENTS UP (2) et la Gateway répartit les appels.
# À arrêter après la démo (Ctrl+C) : chaque instance a sa propre base H2 en mémoire.
cd "$(dirname "$0")" || exit 1
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
exec $JAVA_HOME/bin/java -jar service-clients/target/service-clients-0.0.1-SNAPSHOT.jar --server.port=8091
