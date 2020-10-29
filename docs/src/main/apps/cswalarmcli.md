# csw-alarm-cli

A command line application that facilitates interaction with the Alarm Service. It accepts various commands to load and retrieve alarm data,
to subscribe to severity and health activities of alarm, and to change current of the state of alarms.

## Prerequisite

- Location server should be running.
- Alarm Service should be running.

## Running latest release of alarm-cli using Coursier
@@@ note

This page assumes that you have already installed and set-up coursier : @ref:[coursier-installation](csinstallation.md) { open=new }.

@@@

### Install alarm-cli app

Following command creates an executable file named alarm-cli in the default installation directory.

```bash
cs install alarm-cli:<version | SHA>
```

Note: If you don't provide the version or SHA in above command, `alarm-cli` will be installed with the latest tagged binary of `csw-alarm-cli`


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
The commands listed below will be used by administrators of the Alarm Service.

## init
Loads the alarm data in the alarm store

* `file path` - is a required parameter. Can be on local disk or in the Config Service server (by default it will be picked from the Config Service.)
* `--local` - this specifies that the config file must be read from the local disk
* `--reset` - this is an optional parameter to clear previous data before loading the new one. By default, it is `false`.

#### Example

The command below clears the alarm store and loads alarm data to the alarm store from `/path/allAlarms.conf`, which is the path of a local file.
```bash
alarm-cli init /path/allAlarms.conf --local --reset
```

## list
Gets the data of alarms from the alarm store. If none of the optional parameters are given, then by default, data of all alarms
will be displayed.

* `--subsystem` - is an optional parameter to get the data of a specific subsystem's alarms
* `--component` - is an optional parameter to get the data of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the data of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.
* `--metadata` - is an optional parameter to get only the metadata of alarms
* `--status` - is an optional parameter to get only the status of the alarms

#### Examples

1.  Displays metadata, status and severity of alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
    ```bash
    alarm-cli list --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
    ```


2.  Displays metadata and status of all alarms which belong to the `nfiraos` subsystem.
    ```bash
    alarm-cli list --subsystem nfiraos --metadata --status
    ```

### Operations specific to an alarm

Commands given below are specific to an alarm. All of commands must be provided with `--subsystem`,`--component` and `--name` as parameters.

* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name of the alarm

## acknowledge
Sets the acknowledgement status of the alarm to `Acknowledged`

#### Example

Acknowledge the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli acknowledge --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## unacknowledge
Sets the acknowledgement status of the alarm to `Unacknowledged`

#### Example

Unacknowledge the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli unacknowledge --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## activate
Sets the activation status of the alarm to `Active`

#### Example

Sets activation status to `Active` of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli activate --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## deactivate
Sets the activation status of the alarm to `Inactive`

#### Example

Sets activation status to `Inactive` of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli inactivate --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## shelve
Sets the shelve status of the alarm to `Shelved`

#### Example

Shelves the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli shelve --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## unshelve
Sets the shelve status of the alarm to `Unshelved`

#### Example

Unshelves the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli unshelve --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## reset
Resets the alarm status. This will set the acknowledgement Status to `Acknowledged` and the latched severity to the current
severity of the alarm.

#### Example

Resets the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
```bash
alarm-cli reset --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
```

## severity

Severity command contains 3 sub-commands.

### get
Used to get the severity of the subsystem, component or alarm. If none of the optional parameters are given, then the severity
of whole TMT system will be displayed.

@@@ note

For a single alarm, current severity will be displayed. For a system, subsystem or component, aggregated severity will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is an optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### Examples

1.  Displays the severity of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
    ```bash
    alarm-cli severity get --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
    ```


2.  Displays the aggregated severity of the component with name `trombone` of subsystem `nfiraos`.
    ```bash
    alarm-cli severity get --subsystem nfiraos --component trombone
    ```

### set
Sets the given severity for the specified alarm

* severity to which given alarm needs to be set
* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name alarm
* `--refresh` - is an optional parameter to refresh severity after every 3 seconds

#### Examples

1.  Sets `Major` as the severity of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
    ```bash
    alarm-cli severity set major --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
    ```


2.  Refresh `Major` as the severity of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos` 
every 3 seconds.
    ```bash
    alarm-cli severity set major --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm --refresh
    ```

### subscribe
Subscribes to the severity changes of the whole TMT system, subsystem, component or an alarm. If none of the optional parameters
are given then the severity of the whole TMT system will be displayed.

@@@ note

For a single alarm, the current severity will be displayed. For system, subsystem or component, the aggregated severity will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is an optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### Example

Subscribes to the aggregated severity of a component with name `trombone` and subsystem `nfiraos`.
```bash
alarm-cli severity subscribe --subsystem nfiraos --component trombone
```


## health

Health command contains two sub-commands. 

### get

Gets the health of the whole TMT system, subsystem, component or alarm. If none of the optional parameters are given, then
the health of the whole TMT system will be displayed.

@@@ note

For a single alarm, the current health will be displayed. For the system, subsystem or component, the aggregated health will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the health of a specific subsystem's alarms
* `--component` - is an optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the health of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### Examples

1.  Displays the health of the alarm with name `tromboneAxisLowLimitAlarm` of component `trombone` and subsystem `nfiraos`.
    ```bash
    alarm-cli health get --subsystem nfiraos --component trombone --name tromboneAxisLowLimitAlarm
    ```


2.  Displays the aggregated health of the subsystem with name `nfiraos`.
    ```bash
    //cd to installation directory
    cd /tmt/apps
    
    ./alarm-cli health get --subsystem nfiraos
    ```

### subscribe
Subscribes to the health changes of the whole TMT system, subsystem, component or an alarm. If none of the optional parameters are given, then the health of the whole TMT system will be displayed.

@@@ note

For a single alarm, current health will be displayed. For the system, subsystem or component, the aggregated health will be displayed.

@@@

* `--subsystem` - is an optional parameter to get the health of a specific subsystem's alarms
* `--component` - is an optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is an optional parameter to get the health of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### Examples

Subscribes to the aggregated health of the subsystem with name `nfiraos`.
```bash
alarm-cli health subscribe --subsystem nfiraos --component trombone
```


## About this application

Prints the help message.
```bash
alarm-cli --help
```


Prints the version of the application.
```bash
alarm-cli --version
```


## Running latest master of alarm-cli on developer machine

@@@ note

While development or testing, in order to use this CLI application, below prerequisites must be satisfied:  

*  @ref:[csw-location-server](./../apps/cswlocationserver.md) application is running.
*  @ref:[csw-location-agent](./../apps/cswlocationagent.md) application is running, which has started the Alarm Server and registered it to the Location Service.

Please refer to @ref:[Starting Apps for Development](cswservices.md#starting-apps-for-development) section for more details on how to start these applications using `csw-services`.

@@@

1.  To run the latest master on dev machine the command `sbt run` can be used.
    ```bash
      // run alarm-cli using sbt with arguments
      sbt "csw-alarm-cli/run list --subsystem nfiraos --metadata --status"
    ```


2. Alternatively the command `sbt publishLocal` followed by `cs launch alarm-cli:0.1.0-SNAPSHOT` can be used.
   Command line parameters can also be passed while launching SNAPSHOT version using coursier.
    ```bash
      // run alarm-cli using coursier
      cs launch alarm-cli:0.1.0-SNAPSHOT -- list --subsystem nfiraos --metadata --status
    ```   

@@@ note

All the above examples require that `csw-location-server` is running on local machine at `localhost:7654`.
If `csw-location-server` is running on remote machine having Ip address `172.1.1.2`, then you need to pass an additional `--locationHost 172.1.1.2` command line argument.
Example:
`csw-alarm-cli list --locationHost 172.1.1.2`

@@@
