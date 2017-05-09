#!/usr/bin/env bash

unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then
   sudo apt-get install unzip
fi

sbt -DenableCoverage=false integration/clean
sbt -DenableCoverage=false integration/universal:stage
