#!/usr/bin/env bash
wget -q https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.2%2B9/OpenJDK11U-jdk_x64_linux_hotspot_11.0.2_9.tar.gz
tar -xf OpenJDK11U-jdk_x64_linux_hotspot_11.0.2_9.tar.gz
export JAVA_HOME=$PWD/jdk-11.0.2+9
export PATH=$JAVA_HOME/bin:$PATH
java -version
