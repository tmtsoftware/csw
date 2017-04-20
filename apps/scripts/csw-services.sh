#!/usr/bin/env bash
#
# Starts services required by CSW and registers them with the location service.
# This script uses the csw-locatio-agent app to start Redis and register it with the
# Location Service under different names (see below).
#
# Usage is:
#
#  csw-services.sh PATH_TO_CSWLOCATIONAGENT   - to start redis and register it for the event, alarm and telemetry services
#
# The services are registered as:
#   "Event Service"
#   "Telemetry Service"
#   "ALarm Service"
#   "Config Service"
#
# Note that the environment variable CSW_INSTALL must be defined to point to the root of the csw install dir
# (This is usually ../install relative to the csw sources and is created by the install.sh script).
#

PATH_TO_CSWLOCATIONAGENT_APP=$1
SERVICES="Event Service,Alarm Service,Telemetry Service"

PORT=9999

# The command is optional
COMMAND="sleep 10"

# Run a command and register services using csw-location-agent
$PATH_TO_CSWLOCATIONAGENT_APP/csw-location-agent --name "$SERVICES" --port $PORT --command "$COMMAND"

# Other usages
# $PATH_TO_CSWLOCATIONAGENT_APP --name "$SERVICES" --command "$COMMAND"
# $PATH_TO_CSWLOCATIONAGENT_APP --name "$SERVICES" --port $PORT "
# $PATH_TO_CSWLOCATIONAGENT_APP --name "$SERVICES"