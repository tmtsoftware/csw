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
:   @@snip [TimeSchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/TimeSchedulerExamples.scala) { #create-scheduler }

Java
:   @@snip [JTimeSchedulerExamples.java](../../../../examples/src/main/java/csw/time/JTimeSchedulerExamples.java) { #create-scheduler }

## TMTTime API

TMTTime represents an instantaneous point in time with nanosecond precision. Its a wrapper around Instant and provides additional information about the timescale of the instant. 
It supports two timescales 
 * Coordinated Universal Time @scaladoc[UTCTime](csw/time/api/models/UTCTime)
 * International Atomic Time @scaladoc[TAITime](csw/time/api/models/TAITime)
 
### Get current UTC time
Gets the current UTC time with nanosecond precision 

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #utc-time }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #utc-time }

### Get current TAI time
Gets the current TAI time with nanosecond precision 

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #tai-time }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #tai-time }

### Conversion from UTC to TAI time
Converts the given UTC time to TAI time.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #utc-to-tai }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #utc-to-tai }

### Conversion from TAI to UTC time
Converts the given @scaladoc[TAITime](csw/time/api/models/TAITime) to @scaladoc[UTCTime](csw/time/api/models/UTCTime).

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #tai-to-utc }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #tai-to-utc }


## TMTTimeHelper API

This API provides additional Zone related functionality on top of TMTTime. It allows users to get a Zoned representation of TMTTime. 

### At local timezone
Gets the given TMTTime at Local timezone. The local timezone is fetched from the System's default timezone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-local }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-local }

### At Hawaii (HST) timezone
Gets the given TMTTime at Hawaii timezone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-hawaii }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-hawaii }

### At Custom timezone
Gets the given TMTTime at the specified timezone.

Scala
:   @@snip [TMTTimeExamples.scala](../../../../examples/src/main/scala/csw/time/TMTTimeExamples.scala) { #at-zone }

Java
:   @@snip [JTMTTimeExamples.java](../../../../examples/src/main/java/csw/time/JTMTTimeExamples.java) { #at-zone }
