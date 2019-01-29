#!/usr/bin/env bash

if [ "$#" == 1 ]
then
mkdir -p ../target/coursier/stage/bin
cp coursier ../target/coursier/stage/bin

./coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-server:"$1" -M csw.location.server.Main -o ../target/coursier/stage/bin/csw-location-server
./coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-agent:"$1" -M csw.location.agent.Main -o ../target/coursier/stage/bin/csw-location-agent
./coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-config-server:"$1" -M csw.config.server.Main -o ../target/coursier/stage/bin/csw-config-server

cp -r ./conf ../target/coursier/stage
cp ./csw-auth/prod/configure.sh ../target/coursier/stage/bin
cp ./csw-services.sh ../target/coursier/stage/bin
cp ./redis-sentinel-prod.sh ../target/coursier/stage/bin
echo "Artifacts successfully generated"

elif [ "$#" -gt 1 ]
then
mkdir -p "$2"/target/coursier/stage/bin
cp "$2"/scripts/coursier "$2"/target/coursier/stage/bin

"$2"/scripts/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-server:"$1" -M csw.location.server.Main -o "$2"/target/coursier/stage/bin/csw-location-server
"$2"/scripts/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-location-agent:"$1" -M csw.location.agent.Main -o "$2"/target/coursier/stage/bin/csw-location-agent
"$2"/scripts/coursier bootstrap -r jitpack com.github.tmtsoftware.csw::csw-config-server:"$1" -M csw.config.server.Main -o "$2"/target/coursier/stage/bin/csw-config-server

cp -r "$2"/scripts/conf "$2"/target/coursier/stage
cp "$2"/scripts/csw-auth/prod/configure.sh "$2"/target/coursier/stage/bin
cp "$2"/scripts/csw-services.sh "$2"/target/coursier/stage/bin
cp "$2"/scripts/redis-sentinel-prod.sh "$2"/target/coursier/stage/bin
echo "Artifacts successfully generated"

else
echo "[ERROR] Please provide CSW version ID as 1st argument"
echo "[ERROR] Please provide CSW base directory path as 2nd argument"
fi
