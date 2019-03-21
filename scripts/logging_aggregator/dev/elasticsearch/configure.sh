#!/bin/sh

# wait for Elasticsearch to start, then run the setup script to
# create and configure the index.
filebeat -c filebeat-init.yml -v -e setup --pipelines --modules system,postgresql,redis