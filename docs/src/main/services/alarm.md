# Alarm Service

The alarm service deals with alarms in the TMT software system.

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
* **AlarmKey** : Represents the specific alarm.
* **AlarmMetadata** : Represents fixed data of that alarm, which will not change in its lifespan.
* **AlarmStatus** : Represents occasionally changing data of the that alarm, which will be changing depending on the
severity change.
* **AlarmSeverity** : Represents possible severity levels of the alarm.

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

Components should only use the client API which is provided in both Java and  scala. The Admin API may be used from an administrative user interface which is available in scala only.
<!-- The @ref:[CSW Alarm Client CLI application](../apps/cswalarmclientcli.md) is provided with this functionality. -->

@@@

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #create-scala-api }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #create-java-api }

## setSeverity

Used to set the severity of given alarm

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-scala }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-java }

## init alarms

Used to load the alarm data to alarm store

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/services/alarm/AlarmServiceClientExampleApp.scala) { #initAlarms}
