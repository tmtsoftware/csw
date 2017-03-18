#!/usr/bin/env bash

yum -y install unzip
sbt universal:packageBin
cd integration/target/universal/
unzip integration-10000.zip
cd integration-10000/bin