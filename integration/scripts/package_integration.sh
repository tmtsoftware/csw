#!/usr/bin/env bash

sbt -DenableCoverage=false integration/clean
sbt -DenableCoverage=false integration/universal:stage
