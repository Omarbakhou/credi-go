#!/bin/zsh
pkill -f -- '-0.0.1-SNAPSHOT.jar'; cd "$(dirname "$0")" && docker compose down
