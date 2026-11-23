#!/bin/zsh
# Lance l'interface Angular (bonus) sur http://localhost:4200 — les services doivent déjà tourner (./start-all.sh)
cd "$(dirname "$0")/credi-go-front" || exit 1
export PATH="$HOME/.nvm/versions/node/v24.19.0/bin:$PATH"
[ -d node_modules ] || npm install
echo "Interface : http://localhost:4200"
exec node node_modules/@angular/cli/bin/ng.js serve --port 4200 --open
