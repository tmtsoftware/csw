# Location Agent

The @github[csw-location-agent](/csw-location/csw-location-agent) project provides an application used to register and track non-csw services, such as Redis, which is used to implement the Event and Alarm Services.

See @ref:[here](../../apps/cswlocationagent.md) for the command line usage.

The @github[LocationAgent](/csw-location/csw-location-agent/src/main/scala/csw/location/agent/LocationAgent.scala) class executes the given shell command in the background, registers it using the location service HTTP API, then waits for it to complete before unregistering it.

Log messages are configured in @github[application.conf](/csw-location/csw-location-agent/src/main/resources/application.conf) to log only to file (under `$TMT_LOG_HOME/csw/logs`).
