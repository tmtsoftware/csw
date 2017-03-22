#!/usr/bin/env bash

sbt -Dprod.publish=true -DenableCoverage=false publish