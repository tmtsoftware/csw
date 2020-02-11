#!/usr/bin/env bash

fetch_artifacts() {
    VERSION="$1"
    BASE_PATH="$2"
    SCRIPTS_PATH="$BASE_PATH"/scripts
    COURSIER_STAGE_DIR="$BASE_PATH"/target/coursier/stage/
    TARGET_PATH="$COURSIER_STAGE_DIR"/bin
    SENTINEL_CONF="$COURSIER_STAGE_DIR"/conf/redis_sentinel/sentinel.conf
    SENTINEL_TEMPLATE_CONF="$COURSIER_STAGE_DIR"/conf/redis_sentinel/sentinel-template.conf

    mkdir -p "$TARGET_PATH"
    cp "$SCRIPTS_PATH"/coursier "$TARGET_PATH"

    bootstrap() {
        ARTIFACT_ID=$1
        MAIN_CLASS=$2
        "$SCRIPTS_PATH"/coursier bootstrap -f -r jitpack com.github.tmtsoftware.csw::"$ARTIFACT_ID":"$VERSION" -M "$MAIN_CLASS" -o "$TARGET_PATH"/"$ARTIFACT_ID"
    }

    # TODO: Not sure why this does not work like the rest of others
    "$SCRIPTS_PATH"/coursier bootstrap -f -r jitpack com.github.tmtsoftware.csw:csw-location-server_2.13:"$VERSION" com.typesafe.akka:akka-http-spray-json_2.13:10.1.11 -M csw.location.server.Main -o "$TARGET_PATH"/csw-location-server
    bootstrap csw-location-agent csw.location.agent.Main
    bootstrap csw-config-server csw.config.server.Main
    bootstrap csw-config-cli csw.config.cli.Main
    bootstrap csw-event-cli csw.event.cli.Main
    bootstrap csw-alarm-cli csw.alarm.cli.Main

    cp -r "$SCRIPTS_PATH"/conf "$COURSIER_STAGE_DIR"
    mv "$SENTINEL_CONF" "$SENTINEL_TEMPLATE_CONF"
    cp "$SCRIPTS_PATH"/csw-auth/prod/start-aas.sh "$TARGET_PATH"
    cp "$SCRIPTS_PATH"/csw-services.sh "$TARGET_PATH"
    cp "$SCRIPTS_PATH"/redis-sentinel-prod.sh "$TARGET_PATH"
    echo "Artifacts successfully generated at $COURSIER_STAGE_DIR"
}

BOOTSTRAP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
WORK_DIR="$(dirname "$BOOTSTRAP_DIR")"

if [ "$#" == 1 ]; then
    fetch_artifacts "$1" "$WORK_DIR"
elif [ "$#" -gt 1 ]; then
    fetch_artifacts "$1" "$2"
else
    echo "[ERROR] Please provide CSW version ID as 1st argument"
    echo "[ERROR] Please provide CSW base directory path as 2nd argument"
fi
