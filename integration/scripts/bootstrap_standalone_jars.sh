#!/usr/bin/env bash

echo "$0: PATH = $PATH"
INTEGRATION_JAR="com.github.tmtsoftware.csw::integration:0.1.0-SNAPSHOT"

sbt publishLocal
cs update
cs bootstrap -r jitpack $INTEGRATION_JAR -M csw.integtration.apps.AssemblyApp -o target/assembly-app --standalone -f
cs bootstrap -r jitpack $INTEGRATION_JAR -M csw.integtration.apps.TestMultipleNicApp -o target/test-multiple-nic-app --standalone -f
