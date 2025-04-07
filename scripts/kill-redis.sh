#!/bin/sh

set -x
pids=`ps aux | grep 'redis' | grep -v grep | awk '{print $2}'`
test "$pids" != "" && kill $pids
