# Prefix in CSW 2

The use of prefixes has been made more consistent in CSW 2.0.0. Before this update, Prefix was a dot-separated String 
starting with a subsystem, where the component name was considered to be the last item.
The Subsystem was extracted from the String on creation.

e.g IRIS.imager.filterWheelAssembly =>
  Subsystem: IRIS
  Component Name: filterWheelAssembly

There was some confusion about what was the Prefix: the entire string (IRIS.imager.filterWheelAssembly), or everything
except the component name (IRIS.imager)?

With CSW 2, we expect this to be less confusing. Prefix is the whole string and is made up of exactly two parts:
the subsystem (explicitly as a Subsystem type) and a String component name, where everything after the subsystem is considered to 
be the component name.

e.g. IRIS.imager.filterWheelAssembly =>
  Subsystem: IRIS
  Component Name: imager.filterWheelAssembly

Prefixes can be still be constructed using a String, but it must have a part at the beginning before the first dot
that matches one of the valid TMT subsystems as specified in the [Subsystem.scala](($github.base_url$/csw-prefix/shared/src/main/scala/csw/prefix/models/Subsystem.scala)).

This change will have the following effects on your code:

## Component Creation

ComponentInfo files now take a Prefix as a String instead of an ambiguous prefix and a component name. Similarly, 
Container configuration files also reference their components using Prefix instead of component name.

For example, in CSW 1:

```hocon
name = "SampleHcd"
componentType = hcd
behaviorFactoryClassName = "org.tmt.nfiraos.samplehcd.SampleHcdBehaviorFactory"
prefix = "nfiraos.samplehcd"
locationServiceUsage = RegisterOnly
```

becomes in CSW 2:

```hocon
prefix = "nfiraos.samplehcd"
componentType = hcd
behaviorFactoryClassName = "org.tmt.nfiraos.samplehcd.SampleHcdBehaviorFactory"
locationServiceUsage = RegisterOnly
```

## Location Service

A `ComponentId` is now constructed from a Prefix and `ComponentType` instead of a component name String and a `ComponentType`.
This allows registration of components from different subsystems with the same component name. 

For example, in CSW 1:

```scala
val hcdConnection = PekkoConnection(ComponentId("hcd1"), ComponentType.HCD)
```

becomes in CSW 2:

```scala
val hcdConnection = PekkoConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
```

## Alarm Service

Alarms are defined in the Alarm Configuration file using a Prefix instead of a subsystem and component name.

For example, in CSW 1:

```hocon
alarms: [
  {
    subsystem = nfiraos
    component = tromboneAssembly
    name = tromboneAxisLowLimitAlarm
    description = "Warns when trombone axis has reached the low limit"
    location = "south side"
    alarmType = Absolute
    supportedSeverities = [Warning, Major, Critical]
    probableCause = "the trombone software has failed or the stage was driven into the low limit"
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command"
    isAutoAcknowledgeable = false
    isLatchable = true
    activationStatus = Active
  }
]
```

becomes in CSW 2:

```hocon
alarms: [
  {
    prefix = nfiraos.tromboneAssembly
    name = tromboneAxisLowLimitAlarm
    description = "Warns when trombone axis has reached the low limit"
    location = "south side"
    alarmType = Absolute
    supportedSeverities = [Warning, Major, Critical]
    probableCause = "the trombone software has failed or the stage was driven into the low limit"
    operatorResponse = "go to the NFIRAOS engineering user interface and select the datum axis command"
    isAutoAcknowledgeable = false
    isLatchable = true
    activationStatus = Active
  }
]
```

AlarmKey also takes Prefix instead of a subsystem and a component name:

CSW 1:

```scala
val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm")
```

becomes in CSW 2:

```scala
val alarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisLowLimitAlarm")
```

# Logging Service

The construction of a Logging Service is typically done using a `LoggerFactory`. `LoggerFactory` now takes a Prefix
instead of just a component name String.  For components, this is done automatically for you in the framework.
This has the additional effect that the Subsystem and Component Name are now added to the log messages.

For example, in CSW 1:

```json
{"@componentName":"my-component-name",
 "@host":"INsaloni.local",
 "@name":"LocationServiceExampleClient",
 "@severity":"INFO",
 "@version":"0.1",
 "actor": "pekko://csw-examples-locationServiceClient@10.131.23.195:53618/user/$a",
 "class":"csw.location.LocationServiceExampleClient",
 "file":"LocationServiceExampleClientApp.scala",
 "line":149,
 "message":"Result of the find call: None",
 "timestamp":"2017-11-30T10:58:03.102Z" }
```

becomes in CSW 2:

```json
{"@prefix":"CSW.my-component-name",
 "@subsystem":"CSW",
 "@componentName":"my-component-name",
 "@host":"INsaloni.local",
 "@name":"LocationServiceExampleClient",
 "@severity":"INFO",
 "@version":"0.1",
 "actor":
   "pekko://csw-examples-locationServiceClient@10.131.23.195:53618/user/$a",
 "class":"csw.location.LocationServiceExampleClient",
 "file":"LocationServiceExampleClientApp.scala",
 "line":149,
 "message":"Result of the find call: None",
 "timestamp":"2017-11-30T10:58:03.102Z"
 }
```

Also, configuring default log levels in application configuration files must be specified using the entire Prefix.

CSW 1:

```hocon
component-log-levels {
    trombonehcd = debug
    tromboneassembly = error
  }
```

becomes in CSW 2:

```hocon
component-log-levels {
    TCS.trombonehcd = debug
    TCS.tromboneassembly = error
  }
```

or alternatively:

```hocon
component-log-levels {
    TCS {
        trombonehcd = debug
        tromboneassembly = error
    }
  }
```

In addition, the Prefix and Subsystem classes have been moved from csw.params.core.models to csw.prefix.models, so 
imports will have be updated.
