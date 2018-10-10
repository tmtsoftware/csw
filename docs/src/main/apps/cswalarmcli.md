# csw-alarm-cli

A command line application that facilitates interaction with Alarm Service. It accepts various commands to load and retrieve alarm data,
to subscribe to severity and health activities of alarm, and to change current of the state of alarms.

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

#### Examples
```
csw-alarm-cli init /path/allAlarms.conf --local --reset
```
Clears the alarm store and loads alarm data to alarm store from `/path/allAlarms.conf` which is a path of a local file.

## list
Gets the data of alarms from alarm store. If none of the optional parameters are given then by default, data of all alarms
will be displayed.

* `--subsystem` - is an optional parameter to get the data of a specific subsystem's alarms
* `--component` - is an optional parameter to get the data of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the data of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.
* `--metadata` - is an optional parameter to get only the  metadata of alarms
* `--status` - is an optional parameter to get only the status of the alarms

#### Examples
1.
```
csw-alarm-cli list --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Displays metadata, status and severity of alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

2.
```
csw-alarm-cli list --subsystem nfiraos --metadata --status
```
Displays metadata and status of all alarms which belong to `nfiraos` subsystem.

### Operations specific to an alarm

Commands given below are specific to an alarm. All of commands must be provided with `--subsystem`,`--component` and `--name` as parameters.

* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name of the alarm

## acknowledge
Sets the acknowledgement status of the alarm to `Acknowledged`

#### Examples
```
csw-alarm-cli acknowledge --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Acknowledge the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

## unacknowledge
Sets the acknowledgement status of the alarm to `Unacknowledged`

#### Examples
```
csw-alarm-cli unacknowledge --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Unacknowledge the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

## activate
Sets the activation status of the alarm to `Active`

#### Examples
```
csw-alarm-cli activate --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Sets activation status to `Active` of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

## deactivate
Sets the activation status of the alarm to `Inactive`

#### Examples
```
csw-alarm-cli inactivate --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Sets activation status to `Inactive` of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

## shelve
Sets the shelve status of the alarm to `Shelved`

#### Examples
```
csw-alarm-cli shelve --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Shelves the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

## unshelve
Sets the shelve status of the alarm to `Unshelved`

#### Examples
```
csw-alarm-cli unshelve --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Unshelves the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

## reset
Resets the alarm status. This will set the acknowledgement Status to `Acknowledged` and the latched severity to the current
severity of the alarm.

#### Examples
```
csw-alarm-cli reset --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Resets the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

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

#### Examples
1.
```
csw-alarm-cli severity get --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Displays severity of alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

2.
```
csw-alarm-cli severity get --subsystem nfiraos --component trombone
```
Displays aggregated severity of component with name `trombone` of subsystem `nfiraos`.

### set
Sets the given severity for the alarms

* severity to which given alarm needs to be set
* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name alarm
* `--refresh` - is an optional parameter to refresh severity after every 3 seconds

#### Examples
1.
```
csw-alarm-cli severity set major --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Sets `Major` as the severity of alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.


2.
```
csw-alarm-cli severity set major --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm --refresh
```
Refresh `Major` as the severity of alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos` 
after every 3 seconds.

### subscribe
Subscribes to the severity changes of the whole TMT system, subsystem, component or an alarm. If none of the optional parameters
are given then the severity of the whole TMT system will be displayed.

@@@ note

For a single alarm, current severity will be displayed. For system, subsystem or component, aggregated severity will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is an optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### Examples
1.
```
csw-alarm-cli severity subscribe --subsystem nfiraos --component trombone
```
Subscribes to the aggregated severity of a component with name `trombone` and subsystem `nfiraos`.


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

#### Examples
1.
```
csw-alarm-cli health get --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```
Displays health of alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.

2.
```
csw-alarm-cli health get --subsystem nfiraos
```
Displays aggregated health of subsystem with name `nfiraos`.

### subscribe
Subscribes to the health changes of the whole TMT system, subsystem, component or an alarm. If none of the optional parameters are given then the health of the whole TMT system will be displayed.

@@@ note

For a single alarm, current health will be displayed. For system, subsystem or component, aggregated health will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the health of a specific subsystem's alarms
* `--component` - is an optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the health of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### Examples
1.
```
csw-alarm-cli health subscribe --subsystem nfiraos --component trombone
```
Subscribes to the aggregated health of subsystem with name `nfiraos`.


## About this application

### --help
Prints the help message.

### --version
Prints the version of the application.
