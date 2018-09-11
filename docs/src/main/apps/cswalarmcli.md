# csw-alarm-cli

A command line application that facilitates interaction with Alarm Service. It accepts various commands to load and retrieve alarms data,
to subscribe to severity and health activities of alarm, and to change current state of alarms.

## Supported Commands

Note: Exactly one operation must be specified from this list - `{init | list | acknowledge | unacknowledge | activate | deactivate | shelve | unshelve | reset | severity | health}`

## Admin API
The commands listed below will be used by administrators of the configuration service.

### init
Used to load the alarm data to the alarm store

* `file path` - is a required parameter. Can be of local disk or config server (by default it will be picked from the config service.
* `--local` - this specifies that config file must be picked up from local disk
* `--reset` - this is optional parameter to specify that to clear previous data before loading new data. By default it will be `false`.

### list
Gets the data of alarms from alarm store. If any optional parameter is not given then data of all alarms of system will be shown.

* `--subsystem` - is a optional parameter to get the data of a specific subsystem's alarms
* `--component` - is a optional parameter to get the data of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is a optional parameter to get the data of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.
* `--metadata` - is a optional parameter to get only metadata of the alarms
* `--status` - is a optional parameter to get only status of the alarms

### Operations specific to a alarm

Commands given below are specific to a single alarm. All of them must be provided with `--subsystem`,`--component` and `--name` as parameters.

* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name of the alarm

### 1. acknowledge
Used to set acknowledgement status of the alarm to `Acknowledged`

### 2. unacknowledge
Used to set acknowledgement status of the alarm to `Unacknowledged`

### 3. activate
Used to set activation status of alarm to `Active`

### 4. deactivate
Used to set activation status of alarm to `Inactive`

### 5. shelve
Used to set shelve status of alarm to `Shelved`

### 6. unshelve
Used to set shelve status of alarm to `Unshelved`

### 7. reset
Used to reset the alarm status. This will set acknowledgement Status to `Acknowledged` and latched severity to the current severity of the alarm.


### severity

This contains 3 subcommands.

#### 1. get
Used to get the severity of the subsystem, component or alarm. If any optional parameter is not given then severity of system will be shown.

@@@ note { title=Note }

For a single alarm, changed severity will be given. For a system, subsystem or component aggregated severity will be given.

@@@

* `--subsystem` - is a optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is a optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is a optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.

#### 2. set
Used to raise the given alarm to given severity

* severity to which given alarm needs to br raised
* `--subsystem` - is a parameter to specify the subsystem of alarm
* `--component` - is a parameter to specify the component of alarm
* `--name` - is a parameter to specify the name alarm

#### 3. subscribe
Used to subscribe to the severity changes of the system, subsystem, component or a alarm. If any optional parameter is not given then severity of system will be shown.

@@@ note { title=Note }

For a single alarm, changed severity will be given. For system, subsystem or component aggregated severity will be given.

@@@

* `--subsystem` - is a optional parameter to get the severity of a specific subsystem's alarms
* `--component` - is a optional parameter to get the severity of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is a optional parameter to get the severity of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.


### health
 This contains 2 subcommands.

#### 1. get

Used to get the health of the system, subsystem, component or alarm. If any optional parameter is not given then health of system will be shown.

@@@ note { title=Note }

For a single alarm, changed health will be given. For system, subsystem or component aggregated health will be given.

@@@

* `--subsystem` - is a optional parameter to get the health of a specific subsystem's alarms
* `--component` - is a optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is a optional parameter to get the health of specific a alarm. `--subsystem` and `--component` must be specified with this parameter.

#### 2. subscribe
Used to subscribe to the health changes of the system, subsystem, component or a alarm. If any optional parameter is not given then health of system will be shown.

@@@ note { title=Note }

For a single alarm, changed health will be given. For system, subsystem or component aggregated health will be given.

@@@

* `--subsystem` - is a optional parameter to get the health of a specific subsystem's alarms
* `--component` - is a optional parameter to get the health of a specific component's alarms. `--subsystem` must be specified with this parameter.
* `--name` - is a optional parameter to get the health of a specific alarm. `--subsystem` and `--component` must be specified with this parameter.
