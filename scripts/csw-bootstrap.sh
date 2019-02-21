#!/usr/bin/env bash

fetch_artifacts(){
VERSION="$1"
BASE_PATH="$2"
SCRIPTS_PATH="$BASE_PATH"/scripts
TARGET_PATH="$BASE_PATH"/target/coursier/stage/bin
SENTINEL_CONF="$BASE_PATH"/target/coursier/stage/conf/redis_sentinel/sentinel.conf
SENTINEL_TEMPLATE_CONF="$BASE_PATH"/target/coursier/stage/conf/redis_sentinel/sentinel-template.conf

mkdir -p "$TARGET_PATH"
cp "$BASE_PATH"/scripts/coursier "$TARGET_PATH"

"$SCRIPTS_PATH"/coursier bootstrap -r jitpack -r https://jcenter.bintray.com/ com.github.tmtsoftware.csw::csw-location-server:"$1" -M csw.location.server.Main -o "$TARGET_PATH"/csw-location-server
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-agent:"$1" -M csw.location.agent.Main -o "$TARGET_PATH"/csw-location-agent
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-config-server:"$1" -M csw.config.server.Main -o "$TARGET_PATH"/csw-config-server
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-admin-server:"$1" -M csw.admin.server.Main -o "$TARGET_PATH"/csw-admin-server
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-config-cli:"$1" -M csw.config.cli.Main -o "$TARGET_PATH"/csw-config-cli
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-event-cli:"$1" -M csw.event.cli.Main -o "$TARGET_PATH"/csw-event-cli
"$SCRIPTS_PATH"/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-alarm-cli:"$1" -M csw.alarm.cli.Main -o "$TARGET_PATH"/csw-alarm-cli

cp -r "$SCRIPTS_PATH"/conf "$BASE_PATH"/target/coursier/stage
mv "$SENTINEL_CONF" "$SENTINEL_TEMPLATE_CONF"
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