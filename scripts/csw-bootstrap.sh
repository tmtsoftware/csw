#!/usr/bin/env bash

fetch_artifacts(){
VERSION="$1"
BASE_PATH="$2"
SCRIPTS_PATH="$BASE_PATH"/scripts
TARGET_PATH="$BASE_PATH"/target/coursier/stage/bin

mkdir -p "$TARGET_PATH"
cp "$BASE_PATH"/scripts/coursier "$TARGET_PATH"

"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-server:"$1" -M csw.location.server.Main -o "$TARGET_PATH"/csw-location-server
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-agent:"$1" -M csw.location.agent.Main -o "$TARGET_PATH"/csw-location-agent
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-config-server:"$1" -M csw.config.server.Main -o "$TARGET_PATH"/csw-config-server

cp -r "$SCRIPTS_PATH"/conf "$BASE_PATH"/target/coursier/stage
cp "$SCRIPTS_PATH"/csw-auth/prod/configure.sh "$TARGET_PATH"
cp "$SCRIPTS_PATH"/csw-services.sh "$TARGET_PATH"
cp "$SCRIPTS_PATH"/redis-sentinel-prod.sh "$TARGET_PATH"
echo "Artifacts successfully generated"
}

if [ "$#" == 1 ]
then
fetch_artifacts "$1" ".."
elif [ "$#" -gt 1 ]
then
fetch_artifacts "$1" "$2"
else
echo "[ERROR] Please provide CSW version ID as 1st argument"
echo "[ERROR] Please provide CSW base directory path as 2nd argument"
fi