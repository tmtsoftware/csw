# Alarm Service

The Alarm Service provides API to manage alarms present in the TMT software system.

<!-- introduction to the service -->

## Dependencies

To use the Alarm service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-alarm-client" % "$version$"
    ```

## Rules and checkes
* When representing a unique alarm, the alarm name or component name must not have `* [ ] ^ -` or `any whitespace characters`

## Model Classes
* **AlarmKey** : Represents the unique alarm in the TMT system. It is composed of subsystem, component and alarm name.
* **ComponentKey** : Represents all alarms of a component
* **SubsystemKey** : Represents all alarms of a subsystem
* **GlobalKey** : Represents all alarms present in the TMT system
* **AlarmMetadata** : Represents static metadata of an alarm, which will not change in its entire lifespan.
* **AlarmStatus** : Represents dynamically changing data of the an alarm, which will be changing depending on the
severity change or manually changed by an operator
* **AlarmSeverity** : Represents severity levels that can be set by the component developer e.g. Okay, Indeterminate, 
Warning, Major and Critical 
* **FullAlarmSeverity** : Represents all possible severity levels of the alarm i.e. Disconnected (cannot be set by the developer) 
plus other severity levels that can be set by the developer
* **AlarmHealth** : Represents possible health of an alarm or component or subsystem or whole TMT system

## API Flavours

The Alarm Service is used to manage alarms and its properties. Component developers will get the handle of `CswContext`
which has `alarmService` ready to be used for setting severity.

@@@ note

The `alarmService` provided in `CswContext` is a clientAPI (explained in detail below) which only has `setSeverity` API.

@@@  

<!-- give CLI reference in here -->
To use the alarm service for administrative purposes adminAPI must be used. The creation of the adminAPI is demonstrated below:
The adminAPI provides the ability to load alarm data into alarm store, set severity of an alarm, acknowledge alarms, shelve
or unshelve alarms, reset an alarm, getting the metadata/status/severity of an alarm and getting or subscribing to aggregations of severity and health of the alarm/component/subsystem/whole TMT System.

* **clientAPI (AlarmService)** : Must be used by component. Available method is : `{setSeverity}`
* **adminAPI (AlarmAdminService)** : Expected to be used by administrator. Available methods are: 
`{initAlarm | setSeverity | acknowledge | shelve | unshelve | reset | getMetaData
| getStatus | getCurrentSeverity | getAggregatedSeverity | getAggregatedHealth | subscribeAggregatedSeverityCallback
| subscribeAggregatedSeverityActorRef | subscribeAggregatedHealthCallback | subscribeAggregatedHealthActorRef }`

## Creating clientAPI and adminAPI

If you are not using csw-framework, you can create @scaladoc[AlarmService](csw/alarm/api/scaladsl/AlarmService)
using @scaladoc[AlarmServiceFactory](csw/alarm/client/AlarmServiceFactory).

@@@ note

The adminAPI will be used from an administrative user interface or an alarm CLI. More details about alarm CLI can be 
found here.
 The @ref:[CSW Alarm Client CLI application](../apps/cswalarmcli.md) is provided with this functionality.

@@@

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #create-scala-api }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #create-java-api }

## setSeverity

Sets the severity of the given alarm. It is important that component devs keep refreshing the severity by setting it at 
a regular interval for all its alarms, so that it does not get marked as `Disconnected` after a specific time.  

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-scala }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-java }

@@@ note

* If the alarm is not refreshed within 9 seconds, it will be inferred as `Disconnected`
* If the alarm is auto-acknowledgable and the severity is set to `Okay` then, the alarm will be auto-acknowledged and
  will not require any explicit admin action in terms of acknowledging

@@@

## init alarms

Loads the given alarm data in alarm store

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #initAlarms}

## acknowledge

Acknowledges the given alarm which is raised to a certain severity

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #acknowledge}

## shelve

Shelves the given alarm. Alarms will be un-shelved automatically at a specific time(i.e. 8 AM local time by default) if 
it is not un-shelved manually before that. The time to automatically un-shelve can be configured in application.conf 
for e.g csw-alarm.shelve-timeout = h:m:s a .

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #shelve}

@@@ note

Shelved alarms are also considered in aggregation calculation of alarms.

@@@

## unshelve

Unshelves the given alarm

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #unshelve}


## reset

Resets the status of the given alarm by updating the latched severity same as current severity and acknowledgement status to acknowledged without changing any other properties of the alarm.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #reset}


## getMetadata

Gets the metadata(s) of an alarm/component/subsystem/whole TMT system, which contains fields like:

* subsystem
* component
* name
* description
* location
* alarmType
* supported severities
* probable cause
* operator response
* is autoAcknowledgeable
* is latchable
* activation status

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getMetadata}

@@@ note

Inactive alarms will not be taking part in aggregation of severity or health. 

@@@

## getStatus
Gets the status of the alarm which contains fields like:

* latched severity
* acknowledgement status
* shelve status
* alarm time

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getStatus}

## getCurrentSeverity

Gets the severity of the alarm.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getCurrentSeverity}

## getAggregatedSeverity

Gets the aggregated severity for the given alarm/component/subsystem/whole TMT system. Aggregation of the severity represents
the most severe alarm amongst multiple alarms.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getAggregatedSeverity}


## getAggregatedHealth

Gets the aggregated health for the given alarm/component/subsystem/whole TMT system. Aggregation of health is either `Good`, `ill`
or `Bad` based on the most severe alarm amongst multiple alarms.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getAggregatedHealth}


## subscribeAggregatedSeverityCallback

Subscribes to the changes of aggregated severity for given alarm/component/subsystem/whole TMT system by providing a callback
which gets executed for every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedSeverityCallback}


## subscribeAggregatedSeverityActorRef

Subscribes to the changes of aggregated severity for given alarm/component/subsystem/whole TMT system by providing an actor
which will receive a message of aggregated severity on every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedSeverityActorRef}


## subscribeAggregatedHealthCallback

Subscribe to the changes of aggregated health for given alarm/component/subsystem/whole TMT system by providing a callback
which gets executed for every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedHealthCallback}


## subscribeAggregatedHealthActorRef

Subscribes to the changes of aggregated health for given alarm/component/subsystem/whole TMT system by providing an actor 
which will receive a message of aggregated severity on every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedHealthActorRef}
