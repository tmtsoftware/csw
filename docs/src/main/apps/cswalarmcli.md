# csw-alarm-cli

A command line application that facilitates interaction with Alarm Service. It accepts various commands to load and retrieve alarm data,
to subscribe to severity and health activities of alarm, and to change current the state of alarms.

## Supported Commands

* init 
* list 
* acknowledge 
* unacknowledge 
* activate 
* deactivate 
* shelve 
* unshelve 
* reset 
* severity 
* health

## Admin API
The commands listed below will be used by administrators of the alarm service.

## init
Loads the alarm data in the alarm store

* `file path` - is a required parameter. Can be of local disk or config server (by default it will be picked from the config service.
* `--local` - this specifies that config file must be picked up from local disk
* `--reset` - this is an optional parameter to clear previous data before loading the new one. By default it will be `false`.

## list
Gets the data of alarms from alarm store. If none of the optional parameters are given then by default data of all alarms
will be displayed.

* `--subsystem` - is an optional parameter to get the data of a specific subsystem's alarms
* `--component` - is an optional parameter to get the data of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the data of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.
* `--metadata` - is an optional parameter to get only the  metadata of alarms
* `--status` - is an optional parameter to get only the status of the alarms

### Operations specific to an alarm

Commands given below are specific to a single alarm. All of them must be provided with `--subsystem`,`--component` and `--name` as parameters.

* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name of the alarm

## acknowledge
Sets the acknowledgement status of the alarm to `Acknowledged`

## unacknowledge
Sets the acknowledgement status of the alarm to `Unacknowledged`

## activate
Sets the activation status of the alarm to `Active`

## deactivate
Sets the activation status of the alarm to `Inactive`

## shelve
Sets the shelve status of the alarm to `Shelved`

## unshelve
sets the shelve status of the alarm to `Unshelved`

## reset
Resets the alarm status. This will set the acknowledgement Status to `Acknowledged` and the latched severity to the current
severity of the alarm.


## severity

Severity command contains 3 sub-commands.

### get
Used to get the severity of the subsystem, component or alarm. If none of the optional parameters are given then severity
of whole TMT system will be displayed.

@@@ note

For a single alarm, current severity will be displayed. For a system, subsystem or component, aggregated severity will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is an optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

### set
Sets the given severity for the alarms

* severity to which given alarm needs to be set
* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name alarm

### subscribe
Subscribes to the severity changes of the whole TMT system, subsystem, component or an alarm. If none of the optional parameters
are not given then the severity of the whole TMT system will be displayed.

@@@ note

For a single alarm, current severity will be displayed. For system, subsystem or component, aggregated severity will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is an optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.


## health

Health command contains two sub-commands. 

### get

Gets the health of the whole TMT system, subsystem, component or alarm. If none of the optional parameters are given then
the health of the whole TMT system will be displayed.

@@@ note

For a single alarm, current health will be displayed. For system, subsystem or component, aggregated health will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the health of a specific subsystem's alarms
* `--component` - is an optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the health of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

### subscribe
Subscribes to the health changes of the whole TMT system, subsystem, component or an alarm. If none of the optional parameters are given then
the health of the whole TMT system will be displayed.

@@@ note

For a single alarm, current health will be displayed. For system, subsystem or component, aggregated health will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the health of a specific subsystem's alarms
* `--component` - is an optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the health of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.
