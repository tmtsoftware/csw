# Time Service

The Time Service provides various APIs to access time in Coordinated Universal Time (UTC) and International Atomic Time (TAI) time scales.
It also provides APIs for scheduling periodic and non-periodic tasks in future which are optimised for scheduling at 1KHz frequency.

The Time Service provide APIs for managing time across various components within the TMT architecture. 
TMT has standardised on the use of Precision Time Protocol (PTP) as the basis of observatory time to achieve sub-microsecond accuracy and precision.
PTP provides two mechanisms namely, hardware time stamping and software time stamping to achieve such a high accuracy.
The Global Positioning System (GPS) would provide the absolute time base called Observatory Time.
The PTP grand master clock (a hardware device) is synchronized to Observatory Time. Each computer system participating in the PTP network synchronizes to Observatory Time using the PTP protocol.
For higher accuracy in time measurements hardware time stamping is recommended and the systems should be fitted with PTP capable Network Interface Cards (NIC).
The time service relies on making native calls (linux kernel C methods invocation) to get the nanosecond precise time overcoming the limitations of scala and java libraries which support only microseconds till date.
The native calls are wrapped around java and scala APIs for easy use by component developers. The time service is responsible for primarily providing time in Coordinated Universal Time (UTC) and International Atomic Time (TAI) time scales.
The time service also allows for scheduling tasks either periodically or once using both UTC and TAI time.
These schedulers are optimised for handling scheduled task at 1KHz frequency or 1 task every 1 millisecond. However, there can be jitters due to JVM garbage collection, CPU loads and concurrent task execution.

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

There are total three APIs provided in the Time Service: TMTTime API, TMTTimeHelper API and a Scheduler API.  

* **TMTTime API** : This API provides a way to get current UTC or TAI time. It also provides APIs to convert UTCTime to TAITime and vice-versa.
* **TMTTimeHelper API** : This API provides additional Zone related functionality on top of TMTTime. It allows users to get a Zoned representation of UTCTime. 
* **Scheduler API** : This API provides various methods to schedule future or periodic tasks. 

## Creating APIs

For component developers, the scheduler API is provided as an @scaladoc[TimeServiceScheduler](csw/time/api/TimeServiceScheduler) 
object in the `CswContext` object injected into the ComponentHandlers class provided by the framework.  

If you are not using csw-framework, you can create @scaladoc[TimeServiceScheduler](csw/time/api/TimeServiceScheduler)
using @scaladoc[TimeServiceSchedulerFactory](csw/time/client/TimeServiceSchedulerFactory).

Scala
:   @@snip [TimeSchedulerExamples.scala](../../../../examples/src/main/scala/csw/time/TimeSchedulerExamples.scala) { #create-scheduler }

Java
:   @@snip [JTimeSchedulerExamples.scala](../../../../examples/src/main/java/csw/time/JTimeSchedulerExamples.java) { #create-scheduler }