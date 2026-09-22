#!/usr/bin/env bash
set -euo pipefail

if [ -d api/src/main/java/com/weaponsregistration ]; then
  git mv api/src/main/java/com/weaponsregistration api/src/main/java/com/comandos
fi
if [ -d api/src/test/java/com/weaponsregistration ]; then
  git mv api/src/test/java/com/weaponsregistration api/src/test/java/com/comandos
fi

find api/src -type f -name '*.java' -print0 | xargs -0 sed -i 's/com\.weaponsregistration/com.comandos/g'

if [ -f api/src/main/java/com/comandos/WrApiApplication.java ]; then
  git mv api/src/main/java/com/comandos/WrApiApplication.java api/src/main/java/com/comandos/ComandosApiApplication.java
  sed -i 's/WrApiApplication/ComandosApiApplication/g' api/src/main/java/com/comandos/ComandosApiApplication.java
fi

if [ -f api/src/test/java/com/comandos/WrApiApplicationTests.java ]; then
  git mv api/src/test/java/com/comandos/WrApiApplicationTests.java api/src/test/java/com/comandos/ComandosApiApplicationTests.java
  sed -i 's/WrApiApplicationTests/ComandosApiApplicationTests/g' api/src/test/java/com/comandos/ComandosApiApplicationTests.java
fi

if [ -f app/package-lock.json ]; then
  sed -i 's/"name": "wr-app"/"name": "comandos-app"/g' app/package-lock.json
fi

git add api app/package-lock.json
if git diff --cached --quiet; then
  echo "Migration already applied."
  exit 0
fi

git config user.name "COMANDOS Architecture Bot"
git config user.email "actions@users.noreply.github.com"
git commit -m "069 - Migra namespace legado para COMANDOS [skip ci]"
git push origin HEAD:migration/modular-platform
