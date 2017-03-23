#!/usr/bin/env bash

yum -y install unzip
sbt -DenableCoverage=false universal:packageBin
echo "All" | unzip integration/target/universal/integration-10000.zip -d integration/target/universal/