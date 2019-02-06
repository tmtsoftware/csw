# Time Service

The Time Service provides APIs to access time in Coordinated Universal Time (UTC) and International Atomic Time (TAI) time scales with up to nano 
second precision when available. 
It also provides APIs for scheduling periodic and non-periodic tasks in the future, which are optimised for scheduling at up to 1KHz frequency.

TMT has standardised on the use of [Precision Time Protocol (PTP)](https://en.wikipedia.org/wiki/Precision_Time_Protocol) as the basis of time 
to achieve sub-microsecond accuracy and precision between computers. The Time Service provides each participating computer with access to time synchronized by PTP. 

At the telescope site, the Global Positioning System (GPS) provides an absolute time base, and a
PTP grand master clock (a hardware device) synchronized to the GPS broadcasts the PTP protocol. 
Each computer system participating in the PTP system is synchronized with GPS and each other using the PTP protocol. 
For higher accuracy in time measurements hardware time stamping is required, and those computers should be fitted 
with PTP capable Network Interface Cards (NIC).

In order to read the time with high precision, the Time Service relies on making native calls to the Linux kernel libraries, 
since Java 8 supports only millisecond precision. [Java Native Access (JNA)](https://github.com/java-native-access/jna) 
is used internally in Time Service to make native calls that return the required precision.
The implementation of Time Service Scheduler is based on the [Akka Scheduler](https://doc.akka.io/docs/akka/current/scheduler.html),
which is designed for high-throughput tasks rather than long-term, cron-like scheduling of tasks.

<!-- introduction to the service -->

## Dependencies

The Time Service comes bundled with the Framework, no additional dependency needs to be added to your `build.sbt`
 file if using it.  To use the Time Service without using the framework, add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-time-client" % "$version$"
    ```
    @@@

## Time Service API Flavors

There are total three APIs provided by the Time Service:  

* **TMTTime API**: This API provides a way to get current UTC or TAI time.
* **TMTTimeHelper API**: This API provides additional time zone related functionality on top of TMTTime.
* **Scheduler API**: This API provides various methods to schedule future or periodic tasks. 


### TMTTime API

TMTTime represents an instantaneous point in time with nanosecond precision. It's a wrapper around Instant and provides additional information 
about the timescale of the instant. 

TMTTime supports two timescales:

* Coordinated Universal Time ( @scaladoc[UTCTime](csw/time/api/models/UTCTime) )
* International Atomic Time ( @scaladoc[TAITime](csw/time/api/models/TAITime) )
 
#### Get Current Time
Gets the current UTC/TAI time with nanosecond precision. 

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #current-time }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #current-time }

Note that time is returned a UTCTime or TAITime object so that it is possible to determine the time scale of the
time value by inspection.

#### Converting from UTC to TAI Time and Vice-versa
Each time object provides a way to convert to the other.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #conversion }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #conversion }


### TMTTimeHelper API

This API provides additional time zone related functionality on top of TMTTime. It allows users to get a 
Java *ZonedDateTime* representation of a TMTTime.

#### At Local Time Zone
Gets the given TMTTime at Local time zone. The local time zone is fetched from the calling system's default time zone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-local }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-local }

#### At Hawaii (HST) Timezone
Gets the given TMTTime at Hawaii time zone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-hawaii }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-hawaii }

#### At Custom Timezone
Gets the given TMTTime at the specified time zone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-zone }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-zone }

### Scheduler API

This API provides various methods to schedule periodic or non-periodic, one-shot tasks in the future. 

For component developers, the scheduler API is provided as a @scaladoc[TimeServiceScheduler](csw/time/api/TimeServiceScheduler) 
object in the `CswContext` object injected into the ComponentHandlers class provided by the framework.  

If you are not using csw-framework, you can create @scaladoc[TimeServiceScheduler](csw/time/api/TimeServiceScheduler)
using @scaladoc[TimeServiceSchedulerFactory](csw/time/client/TimeServiceSchedulerFactory) as follows.

Scala
:   @@snip [TimeSchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #create-scheduler }

Java
:   @@snip [JTimeSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #create-scheduler }

For all scheduler calls, an instance of @scaladoc[Cancellable](csw/time/client/api/Cancellable) is returned which can be used 
to cancel the execution of the future tasks.

#### Schedule Once
Schedules a task to execute once at the given start time. The `startTime` is a TMTTime and can be either a UTCTime or TAITime.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-once}

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-once }

@@@ note { title=Warning }

Note that callbacks are asynchronous and can be potentially executed in an unknown thread. Therefore, if there is a need
to mutate state when the time expires, it is recommended to send a message to an Actor, and keep any mutable state within
the actor where it can be managed safely. This is true for any CSW API with a callback. The schedule once with ActorRef can
often be used in this scenario.

@@@

#### Schedule Once With ActorRef
Schedules sending of the message to the provided `actorRef` at the given start time. The `startTime` can be either UTCTime or TAITime.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-once-with-actorRef }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-once-with-actorRef }

#### Schedule Periodically
Schedules a task to execute periodically at the given interval. The first task is executed once immediately 
without any initial delay followed by periodic executions. In case you do not want to start scheduling immediately, 
you can use the overloaded method  for `schedulePeriodically()` with `startTime` as shown in the next example.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-periodically }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-periodically }


#### Schedule Periodically with Start Time
Schedules a task to execute periodically at the given interval. The task is executed once at the given start time followed by 
execution of task at each interval. The `startTime` can be either UTCTime or TAITime.

Scala
:   @@snip [SchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala) { #schedule-periodically-with-startTime }

Java
:   @@snip [JSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java) { #schedule-periodically-with-startTime }

As with the schedule once API, there is also a periodic schedule API that takes a message and ActorRef.

## Source code for TMTTime examples

* @github[Scala Example](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala)
* @github[Java Example](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java)

## Source code for Scheduler examples

* @github[Scala Example](../../../../examples/src/main/scala/csw/time/SchedulerExamples.scala)
* @github[Java Example](../../../../examples/src/main/java/csw/time/JSchedulerExamples.java)
