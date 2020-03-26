#!/usr/bin/env bash

sbt integration/clean
sbt integration/universal:stage
