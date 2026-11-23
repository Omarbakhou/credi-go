#!/bin/zsh
pkill -f 'credi-go/.*-0.0.1-SNAPSHOT.jar'; cd "$(dirname "$0")" && docker compose down
