#!/usr/bin/env bash

sbt -Dcheck.cycles=false -Dprod.publish=true -DenableCoverage=false publish