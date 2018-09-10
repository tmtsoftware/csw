# Alarm Service

The Alarm Service deals with alarms present in the TMT software system.

<!-- introduction to the service -->

## Dependencies

To use the Alarm service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-alarm-client" % "$version$"
    ```

## Rules and checkes
* The alarm name or component name must not have `* [ ] ^ -` or `any whitespace characters`

## Model Classes
* **AlarmKey** : Represents the specific alarm
* **ComponentKey** : Represents all alarms of a component
* **SubsystemKey** : Represents all alarms of a subsystem
* **GlobalKey** : Represents all alarms present in the system
* **AlarmMetadata** : Represents fixed data of that alarm, which will not change in its lifespan.
* **AlarmStatus** : Represents occasionally changing data of the that alarm, which will be changing depending on the
severity change.
* **FullAlarmSeverity** : Represents possible severity levels of the alarm
* **AlarmHealth** : Represents possible health of the alarm

## API Flavours

The Alarm Service is used to deal with the alarms of the component. When a component is started, it will use the
"clientAPI" to update severity of its alarms.

<!-- give cli reference in here -->
To have a admin level control over alarm service, an administrative tool with access to the full "adminAPI" must be used.
These tool would have the ability to load alarm data into alarm store, set severity of an alarm, acknowledge alarms, shelve
or unshelve alarms, reset alarm status. With this, it also provides different APIs for observing the alarms like getting
metadata, status, severity of an alarm and also subscribing to aggregations of severity or health of the
alarm/component/subsystem/System.

* **clientAPI** : Must be used by component. Available method is : `{setSeverity}`
* **adminAPI** : Full functionality exposed by alarm service server is available with this APIs. Expected to be used by
administrative. Available methods are: `{initAlarm | setSeverity | acknowledge | shelve | unshelve | reset | getMetaData
| getStatus | getCurrentSeverity | getAggregatedSeverity | getAggregatedHealth | subscribeAggregatedSeverityCallback
| subscribeAggregatedSeverityActorRef | subscribeAggregatedHealthCallback | subscribeAggregatedHealthActorRef }`

## Accessing clientAPI and adminAPI

If you are not using csw-framework, you can create @scaladoc[AlarmService](csw/services/alarm/api/scaladsl/AlarmService)
using @scaladoc[AlarmServiceFactory](csw/services/alarm/AlarmServiceFactory).

@@@ note

Components should only use the client API which is provided in both Java and scala. The Admin API may be used from an administrative user interface which is available in scala only.
<!-- The @ref:[CSW Alarm Client CLI application](../apps/cswalarmclientcli.md) is provided with this functionality. -->

@@@

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #create-scala-api }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #create-java-api }

## setSeverity

Used to set the severity of given alarm in the alarm store

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-scala }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-java }

## init alarms

Used to load the given alarm data in alarm store

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #initAlarms}

## acknowledge

Used to acknowledge the given alarm which is raised to a certain severity

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #acknowledge}


## shelve

Used to shelve the given alarm which is raised to a certain severity. Alarm will be unshelved on the configured time(which is 8 Am by default). Shelved alarms are also considered in aggregation calculation of alarms

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #shelve}


## unshelve

Used to explicitly unshelve the given alarm

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #unshelve}


## reset

Used to reset the status of the given alarm. It will set the latched severity to current severity and acknowledgement status to acknowledged, other fields remains the same.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #reset}


## getMetaData

Gets the metadata(s) for alarm/component/subsystem/system. Metadata of an alarm contains following fields.
- subsystem
- component
- name
- description
- location
- alarmType
- supported severities
- probable cause
- operator response
- is autoAcknowledgeable
- is latchable
- activation status

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #getMetadata}

## getStatus
Gets the status of the alarm. Status contains following fields.
- latched severity
- acknowledgement status
- shelve status
- alarm time

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #getStatus}

## getCurrentSeverity

Gets the severity of the alarm

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #getCurrentSeverity}

## getAggregatedSeverity

Gets the aggregated severity for the given alarm/component/subsystem/system. Aggregation of severity is done to the most sever severity.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #getAggregatedSeverity}


## getAggregatedHealth

Gets the aggregated health for the given alarm/component/subsystem/system. Aggregation of health is done by mapping most sever severity to health.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #getAggregatedHealth}


## subscribeAggregatedSeverityCallback

Used to subscribe to the changes of aggregated severity for given alarm/component/subsystem/system. The given callback will be executed whenever aggregated severity changes with new aggregated severity as a parameter.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedSeverityCallback}


## subscribeAggregatedSeverityActorRef

Used to subscribe to the changes of aggregated severity for given alarm/component/subsystem/system. The given actor will be sent a message of aggregated severity on every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedSeverityActorRef}


## subscribeAggregatedHealthCallback

Used to subscribe to the changes of aggregated health for given alarm/component/subsystem/system. The given callback will be executed whenever aggregated health changes with new aggregated health as a parameter.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedHealthCallback}


## subscribeAggregatedHealthActorRef

Used to subscribe to the changes of aggregated health for given alarm/component/subsystem/system. The given actor will be sent a message of aggregated health on every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedHealthActorRef}
