#!/bin/sh

set -x
pids=`ps aux | grep 'redis' | grep -v grep | awk '{print $2}'`
test "$pids" != "" && kill $pids
pids=`ps aux | grep 'redis' | grep -v grep | awk '{print $2}'`
test "$pids" != "" && echo "These redis processes are still running: $pids"
exit 0

