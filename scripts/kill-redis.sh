#!/bin/sh

pids=`$(ps aux | grep 'redis' | grep -v grep | awk '{print $2}')`
if "$pids" != "";
  echo "Killing existing redis instances"
  kill $pids
fi
