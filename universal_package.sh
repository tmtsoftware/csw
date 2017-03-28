#!/usr/bin/env bash

yum -y install unzip
sbt -DenableCoverage=false universal:packageBin
echo "All" | unzip integration/target/universal/integration-0.1-SNAPSHOT.zip -d integration/target/universal/