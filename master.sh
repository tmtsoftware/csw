#!/usr/bin/env bash

git checkout .
git pull --rebase

sbt clean compile test
