# Time Service

The Time Service provides various APIs to access time in Coordinated Universal Time (UTC) and International Atomic Time (TAI) time scales with nano second precision. 
It also provides APIs for scheduling periodic and non-periodic tasks in future which are optimised for scheduling at upto 1KHz frequency.

TMT has standardised on the use of [Precision Time Protocol (PTP)](https://en.wikipedia.org/wiki/Precision_Time_Protocol) as the basis of observatory time to achieve sub-microsecond accuracy and precision. The Time Service provides access to time synchronized by PTP. 
The Global Positioning System (GPS) provides the absolute time base called Observatory Time. The PTP grand master clock (a hardware device) is synchronized to Observatory Time. Each computer system participating in the PTP system synchronizes to Observatory Time using the PTP protocol. For higher accuracy in time measurements hardware time stamping is recommended and the systems should be fitted with PTP capable Network Interface Cards (NIC).

In order to read the time with high precision, the Time Service relies on making native calls to the Linux Kernel libraries, since java 8 supports only millisecond precision. [Java Native Access (JNA)](https://github.com/java-native-access/jna) is used internally in time service to make native calls.
The implementation of time service scheduler is based on the [Akka Scheduler](https://doc.akka.io/docs/akka/current/scheduler.html) which is designed for high-throughput tasks rather than long-term scheduling.

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

It supports two timescales:

* Coordinated Universal Time ( @scaladoc[UTCTime](csw/time/api/models/UTCTime) )
* International Atomic Time ( @scaladoc[TAITime](csw/time/api/models/TAITime) )
 
### Get Current Time
Gets the current UTC/TAI time with nanosecond precision. 

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #current-time }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #current-time }

### Conversion from UTC to TAI Time and Vice-versa

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #conversion }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #conversion }

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
Schedules sending of the message to the provided `actorRef` at the given start time. The `startTime` can be either UTC time or TAI time.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-once-with-actorRef }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-once-with-actorRef }

### Schedule Periodically
Schedules a task to execute periodically at the given interval. The task is executed once immediately without any initial delay followed by periodic executions. In case you do not want to start scheduling immediately, you can use the overloaded method  for `schedulePeriodically()` with `startTime` as shown in the next example.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-periodically }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-periodically }


### Schedule Periodically with Start Time
Schedules a task to execute periodically at the given interval. The task is executed once at the given start time followed by execution of task at each interval. The `startTime` can be either UTC time or TAI time.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-periodically-with-startTime }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-periodically-with-startTime }


## Source code for TMTTime examples

* @github[Scala Example](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala)
* @github[Java Example](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java)

## Source code for Scheduler examples

* @github[Scala Example](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala)
* @github[Java Example](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java)
