#!/usr/bin/env bash

sudo apt-get install unzip
sbt -DenableCoverage=false integration/clean
sbt -DenableCoverage=false integration/universal:packageBin
echo "All" | unzip integration/target/universal/integration-0.1-SNAPSHOT.zip -d integration/target/universal/