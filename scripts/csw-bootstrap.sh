#!/usr/bin/env bash

if [ "$#" -ne 0 ]
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
else
echo "[ERROR] Please provide CSW version ID as argument"
fi
