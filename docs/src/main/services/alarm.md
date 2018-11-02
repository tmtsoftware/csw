# Alarm Service

The Alarm Service provides API to manage alarms in the TMT software system.  The service uses Redis to store Alarm
data, including the alarm status and associated metadata.  Alarm "keys" are used to access information about an alarm.

<!-- introduction to the service -->

## Dependencies

The Alarm Service comes bundled with the Framework, no additional dependency needs to be added to your `build.sbt`
 file if using it.  To use the Alarm service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-alarm-client" % "$version$"
    ```
    @@@

## API Flavours

There are two APIs provided in the Alarm Service: a client API, and an administrative (admin) API.  The client API is
the API used by component developers to set the severity of an alarm.  This is the only functionality needed by 
component developers.  As per TMT policy, the severity of an alarm must be set periodically (within some time limit) 
in order to maintain the integrity of the alarm status.  If an alarm severity is not refreshed within the time limit, 
currently set at TBD seconds, the severity is set to `Disconnected` by the Alarm Service, which indicates to the operator
that there is some problem with the component's ability to evaluate the alarm status.

The admin API provides all of the functions needed manage the alarm store, as well as 
providing access to monitor alarms for use by an operator or instrument specialist.  The admin API provides the
ability to load alarm data into alarm store, set severity of an alarm, acknowledge alarms, shelve
or unshelve alarms, reset a latched alarm, get the metadata/status/severity of an alarm, and get or subscribe to 
aggregations of severity and health of the alarm, a component's alarms, a subsystem's alarms, or the alarms of the 
whole TMT System.  

A command line tool is provided as part of the Alarm Service that implements this API can provides low level control over the
Alarm Service.  More details about alarm CLI can be found here: @ref:[CSW Alarm Client CLI application](../apps/cswalarmcli.md)

Eventually, operators will use Graphical User Interfaces that access the admin API through a UI
gateway.  This will be delivered as part of the ESW HCMS package.

@@@ note
Since the admin API will primarily be used with the CLI and HCMS applications, it is only supported in Scala, and not Java.
@@@

To summarize, the APIs are as follows:
* **client API (AlarmService)** : Must be used by component. Available method is : `{setSeverity}`
* **admin API (AlarmAdminService)** : Expected to be used by administrator. Available methods are: 
`{initAlarm | setSeverity | acknowledge | shelve | unshelve | reset | getMetaData
| getStatus | getCurrentSeverity | getAggregatedSeverity | getAggregatedHealth | subscribeAggregatedSeverityCallback
| subscribeAggregatedSeverityActorRef | subscribeAggregatedHealthCallback | subscribeAggregatedHealthActorRef }`

## Creating clientAPI and adminAPI

For component developers, the client API is provided as an @scaladoc[AlarmService](csw/alarm/api/scaladsl/AlarmService) 
object in the `CswContext` object injected into the ComponentHandlers class provided by the framework.  

If you are not using csw-framework, you can create @scaladoc[AlarmService](csw/alarm/api/scaladsl/AlarmService)
using @scaladoc[AlarmServiceFactory](csw/alarm/client/AlarmServiceFactory).

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #create-scala-api }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/java/csw/alarm/JAlarmServiceClientExampleApp.java) { #create-java-api }

## Rules and checkes
* When representing a unique alarm, the alarm name or component name must not have `* [ ] ^ -` or `any whitespace characters`

## Model Classes
* **AlarmKey** : Represents the unique alarm in the TMT system. It is composed of subsystem, component and alarm name.
* **ComponentKey** : Represents all alarms of a component.  Used for getting severity or health of an entire component.
* **SubsystemKey** : Represents all alarms of a subsystem  Used for getting severity or health of an entire subsystem.
* **GlobalKey** : Represents all alarms present in the TMT system.  Used for getting severity or health of an entire observatory.
* **AlarmMetadata** : Represents static metadata of an alarm, which will not change in its entire lifespan.
* **AlarmStatus** : Represents dynamically changing data of the an alarm, which will be changing depending on the
severity change or manually changed by an operator
* **AlarmSeverity** : Represents severity levels that can be set by the component developer e.g. Okay, Indeterminate, 
Warning, Major and Critical 
* **FullAlarmSeverity** : Represents all possible severity levels of the alarm i.e. Disconnected (cannot be set by the developer) 
plus other severity levels that can be set by the developer
* **AlarmHealth** : Represents possible health of an alarm or component or subsystem or whole TMT system

## Client API

### setSeverity

Sets the severity of the given alarm. The severity must be refreshed by setting it at 
a regular interval or it will automatically be changed to `Disconnected` after a specific time.  

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #setSeverity-scala }

Java
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/java/csw/alarm/JAlarmServiceClientExampleApp.java) { #setSeverity-java }

@@@ note

* If the alarm is not refreshed within 9 seconds, it will be inferred as `Disconnected`
* If the alarm is auto-acknowledgable and the severity is set to `Okay` then, the alarm will be auto-acknowledged and
  will not require any explicit admin action in terms of acknowledging

@@@

## Admin API

### initAlarms

Loads the given alarm data in alarm store, passing in the alarm configuration file.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #initAlarms}


Alarm configuration files are written in the HOCON format using the following fields:
* subsystem: subsystem name the alarm belongs to
* component: name of component for the alarm, matching the name in the componentInfo file (see @ref:[Describing Components](../framework/describing-components.md))
* name: name of the alarm
* description: a description of what the alarm represents
* location: physical location within observatory or instrument in which the alarm condition is occuring
* alarmType: the general category for the alarm.
* supportedSeverities: list of non-Okay severities the alarm may become (Warning, Major, Critical).  All alarms are assumed to support Okay, Disconnected, and Indeterminate.
* probableCause: a description of the likely cause of the alarm reaching each severity level
* operatorResponse: instructions or information to help the operator respond to the alarm.
* isAutoAcknowledgable: true/false flag for whether the alarm automatically acknowledges alarm when alarm severity returns to Okay.
* isLatchable: true/false flag whether alarm latches at highest severity until reset.
* activationStatus: true/false flag for whether alarm is currently active (and considered in aggregated severity and health calculations)

alarms.conf
:   @@snip [valid_alarms.conf](../../../../examples/src/main/resources/valid-alarms.conf) {#alarmsconf}

### acknowledge

Acknowledges the given alarm which is raised to a higher severity

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #acknowledge}

### shelve

Shelves the given alarm. Alarms will be un-shelved automatically at a specific time(i.e. 8 AM local time by default) if 
it is not un-shelved manually before that. The time to automatically un-shelve can be configured in application.conf 
for e.g csw-alarm.shelve-timeout = h:m:s a .

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #shelve}

@@@ note

Shelved alarms are also considered in aggregation severity or health calculation of alarms.

@@@

### unshelve

Unshelves the given alarm

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #unshelve}


### reset

Resets the status of the given latched alarm by updating the latched severity same as current severity and acknowledgement status to acknowledged without changing any other properties of the alarm.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #reset}


### getMetadata

Gets the metadata of an alarm, component, subsystem, or whole TMT system.  The following information is returned for each alarm:

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

Inactive alarms will not be taking part in aggregation of severity or health.  Alarms are set active or inactive in the alarm
configuration file, and not through either API.

@@@

### getStatus
Gets the status of the alarm which contains fields like:

* latched severity
* acknowledgement status
* shelve status
* alarm time

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getStatus}

### getCurrentSeverity

Gets the severity of the alarm.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getCurrentSeverity}

### getAggregatedSeverity

Gets the aggregated severity for the given alarm/component/subsystem/whole TMT system. Aggregation of the severity represents
the most severe alarm amongst multiple alarms.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getAggregatedSeverity}


### getAggregatedHealth

Gets the aggregated health for the given alarm/component/subsystem/whole TMT system. Aggregation of health is either `Good`, `ill`
or `Bad` based on the most severe alarm amongst multiple alarms.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #getAggregatedHealth}


### subscribeAggregatedSeverityCallback

Subscribes to the changes of aggregated severity for given alarm/component/subsystem/whole TMT system by providing a callback
which gets executed for every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedSeverityCallback}


### subscribeAggregatedSeverityActorRef

Subscribes to the changes of aggregated severity for given alarm/component/subsystem/whole TMT system by providing an actor
which will receive a message of aggregated severity on every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedSeverityActorRef}


### subscribeAggregatedHealthCallback

Subscribe to the changes of aggregated health for given alarm/component/subsystem/whole TMT system by providing a callback
which gets executed for every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedHealthCallback}


### subscribeAggregatedHealthActorRef

Subscribes to the changes of aggregated health for given alarm/component/subsystem/whole TMT system by providing an actor 
which will receive a message of aggregated severity on every change.

Scala
:   @@snip [AlarmClientExampleTest.scala](../../../../examples/src/main/scala/csw/alarm/AlarmServiceClientExampleApp.scala) { #subscribeAggregatedHealthActorRef}
