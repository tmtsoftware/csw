# Time Service

The Time Service provides various APIs to access time in Coordinated Universal Time (UTC) and International Atomic Time (TAI) time scales. 
It also provides APIs for scheduling periodic and non-periodic tasks in future which are optimised for scheduling at 1KHz frequency.


TMT has standardised on the use of Precision Time Protocol (PTP) as the basis of observatory time to achieve sub-microsecond accuracy and precision.
The time service reads time from the system clock which will be synchronized by [Precision Time Protocol (PTP)](https://en.wikipedia.org/wiki/Precision_Time_Protocol) across all the components within the TMT architecture.
For hardware time-stamping offered by PTP for sub-microsecond accuracy, the systems should be fitted with PTP capable Network Interface Cards (NIC). 

<!-- introduction to the service -->

## Dependencies

The Time Service comes bundled with the Framework, no additional dependency needs to be added to your `build.sbt`
 file if using it.  To use the Time service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-time-client" % "$version$"
    ```
    @@@

## API Flavors

There are total three APIs provided by the Time Service:  

* **TMTTime API** : This API provides a way to get current UTC or TAI time. It also provides APIs to convert UTCTime to TAITime and vice-versa.
* **TMTTimeHelper API** : This API provides additional Zone related functionality on top of TMTTime. It allows users to get a Zoned representation of TMTTime. 
* **Scheduler API** : This API provides various methods to schedule future or periodic tasks. 

## Creating APIs

For component developers, the scheduler API is provided as a @scaladoc[TimeServiceScheduler](csw/time/api/TimeServiceScheduler) 
object in the `CswContext` object injected into the ComponentHandlers class provided by the framework.  

If you are not using csw-framework, you can create @scaladoc[TimeServiceScheduler](csw/time/api/TimeServiceScheduler)
using @scaladoc[TimeServiceSchedulerFactory](csw/time/client/TimeServiceSchedulerFactory) as follows.

Scala
:   @@snip [TimeSchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #create-scheduler }

Java
:   @@snip [JTimeSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #create-scheduler }

## TMTTime API

TMTTime represents an instantaneous point in time with nanosecond precision. Its a wrapper around Instant and provides additional information about the timescale of the instant. 
It supports two timescales 
 * Coordinated Universal Time @scaladoc[UTCTime](csw/time/api/models/UTCTime)
 * International Atomic Time @scaladoc[TAITime](csw/time/api/models/TAITime)
 
### Get Current UTC Time
Gets the current UTC time with nanosecond precision 

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #utc-time }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #utc-time }

### Get Current TAI Time
Gets the current TAI time with nanosecond precision 

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #tai-time }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #tai-time }

### Conversion from UTC to TAI Time
Converts the given UTC time to TAI time.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #utc-to-tai }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #utc-to-tai }

### Conversion from TAI to UTC Time
Converts the given @scaladoc[TAITime](csw/time/api/models/TAITime) to @scaladoc[UTCTime](csw/time/api/models/UTCTime).

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #tai-to-utc }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #tai-to-utc }


## TMTTimeHelper API

This API provides additional Zone related functionality on top of TMTTime. It allows users to get a Zoned representation of TMTTime. 

### At local Timezone
Gets the given TMTTime at Local timezone. The local timezone is fetched from the System's default timezone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-local }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-local }

### At Hawaii (HST) Timezone
Gets the given TMTTime at Hawaii timezone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-hawaii }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-hawaii }

### At Custom Timezone
Gets the given TMTTime at the specified timezone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-zone }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-zone }

## Scheduler API
This API provides various methods to schedule periodic or non-periodic tasks in future. An instance of @scaladoc[Cancellable](csw/time/client/api/Cancellable) is returned which can be used to cancel the execution of the future tasks.

### Schedule Once
Schedules a task to execute once at the given start time. The `startTime` can be either UTC time or TAI time.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-once}

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-once }

### Schedule Once With ActorRef
Schedules sending of the given message to the provided `actorRef` at the given start time. The `startTime` can be either UTC time or TAI time.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-once-with-actorRef }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-once-with-actorRef }

### Schedule Periodically
Schedules a task to execute periodically at the given interval. The task is executed once immediately without any initial delay followed by periodic executions. In case you do not want to start scheduling immediately, you can use the overloaded method  for `schedulePeriodically()` with `startTime` as shown in following examples.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-periodically }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-periodically }



Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-periodically-with-startTime }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-periodically-with-startTime }