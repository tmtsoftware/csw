#!/usr/bin/env bash

yum -y install unzip
sbt -DenableCoverage=false universal:packageBin
unzip integration/target/universal/integration-10000.zip