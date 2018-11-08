# Using Alarms

Alarms are used to indicate when some condition has arisen that warrants operator intervention.  In order to ensure
the integrity of alarms, components are required to broadcast the state of each alarm periodically, so that situations
in which a component is unable to determine and/or broadcast the alarm state occur, and the operator can take
appropriate action.  This means even when situations are normal, the component must continue to publish each alarm 
with an `Okay` severity; otherwise, the alarm severity is automatically marked as `Disconnected`, prompting the operator
to investigate.

The CSW Alarm Service provides two APIs: a "Client API" used by component, and an "Admin API" used to manage the Alarm
Store and for operator use (typically via HCMS user interfaces).  The Client API consists of a single method call, `setSeverity`. 
The Admin API includes methods to set up the Alarm Store, get alarm severities and health of components or subsystems, 
acknowledge alarms, reset latched alarms, and other operator tasks. 

The Admin API can be exercised using the command line tool provided with CSW: `csw-alarm-cli`.  See the @ref:[reference manual](../apps/cswalarmcli.md)
for more info.

More details about the Alarm Service are found in the @ref:[Alarm Service manual](../services/alarm.md).

#### *Tutorial: Using alarms in a component*

We will use our sample Assembly to monitor the counter event it is subscribed to, published by our sample HCD.
Our alarm is based on the value of the counter, with the normal (`Okay`) range of 0 to 10, `Warning` 
range of 11 to 15, `Major` range of 16 to 20, and any other value generating a `Critical` alarm severity.

First, the Alarm Store must be initialized with our alarm using the CLI tool `csw-alarm-cli`.  A configuration file must be
written that describes every alarm that will be used in the system.  For TMT operations, this configuration will be generated
from the ICD-DB models.  For our tutorial, we will use a configuration with only the alarm we will be using.  

alarms.conf
:   @@snip [sample_alarms.conf](../../../../examples/src/main/resources/sample-alarms.conf)

For our tutorial, let's save this file to disk in our resources folder in the `sample-deploy` module
(`sample-deploy/src/main/resources/alarms.conf`).

Now, we will use the CLI tool.  Find it in the `bin` directory of the CSW application package available with the
[release](https://github.com/tmtsoftware/csw/releases) as `csw-alarm-cli`.

Use the `init` command to initialize the Alarm Store (this assumes csw-services is running, which sets up the Redis store
for alarms).

```
csw-alarm-cli init $PROJECTDIR/sample-deploy/src/main/resources/alarms.conf --local
```

where `$PROJECTDIR` is the root directory of your sample project.  The `--local` flag indicates the configuration file
is obtains from disk; omitting it would attempt to find the file in the Configuration Service, as would be done during 
operations.

Now we will add code to our assembly to publish an alarm severity on every counter event.  Let's create some logic
to take the counter as an argument and generate an alarm:

Scala
:   @@snip [SampleAssemblyHandlers.scala](../../../../examples/src/main/scala/nfiraos/sampleassembly/SampleAssemblyHandlersAlarm.scala) { #alarm }

Java
:   @@snip [JSampleAssemblyHandlers.scala](../../../../examples/src/main/java/nfiraos/sampleassembly/JSampleAssemblyHandlersAlarm.java) { #alarm }

This code determines the severity of the alarm based on the rules we established above:

  * `Okay`: 0-10
  * `Warning`: 11-15
  * `Major`: 16-20
  * `Critical`: any other value
  
Now, all we have to do is call this whenever we receive a counter event.  We add a call to the `setCounterAlarm` method 
in the `processEvent` method:

Scala
:   @@snip [SampleAssemblyHandlers.scala](../../../../examples/src/main/scala/nfiraos/sampleassembly/SampleAssemblyHandlersAlarm.scala) { #subscribe }

Java
:   @@snip [JSampleAssemblyHandlers.scala](../../../../examples/src/main/java/nfiraos/sampleassembly/JSampleAssemblyHandlersAlarm.java) { #subscribe }

To see the effect, let's use the CLI to set up a subscription to the alarm.  Note the alarm key is composed of the subsystem (`nfiraos`),
component name (`SampleAssembly` for Scala, `JSampleAssembly` for Java), and the alarm name (`counterTooHighAlarm`).

Scala
:   
```
csw-alarm-cli severity subscribe --subsystem NFIRAOS --component SampleAssembly --name counterTooHighAlarm
```

Java
:   
```
csw-alarm-cli severity subscribe --subsystem NFIRAOS --component JSampleAssembly --name counterTooHighAlarm
```

Note that the alarm severity is currently `Disconnected`.  This is the appropriate state, since we are not running the 
components.  Now, run the Assembly and HCD, and you will see the severity of our alarm updated in the CLI as the severity changes.

