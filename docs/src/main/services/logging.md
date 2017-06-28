# Logging service

Logging Service library provides an advanced logging facility for csw components and services. 
    
## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "org.tmt" %% "csw-logging_$scala.binaryVersion$" % "$version$"
    ```
    @@@

maven
:   @@@vars
    ```xml
    <dependency>
     <groupId>org.tmt</groupId>
     <artifactId>csw-logging_$scala.binaryVersion$</artifactId>
     <version>$version$</version>
     <type>pom</type>
    </dependency>
    ```
    @@@

gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "org.tmt", name: "csw-logging_$scala.binaryVersion$", version: "$version$"
    }
    ```
    @@@
    
    
## Configuration

These are the relevant default configuration values for logging

Scala
:   @@snip [logging.conf](../../../../csw-logging/src/main/resources/logging.conf)

These values can be overridden directly in your reference.conf or application.conf

## Log Levels

Following Log levels are supported by csw-logging library

* FATAL
* ERROR
* WARN
* INFO
* DEBUG
* TRACE

There are separate log levels for the logging API(logLevel), Akka logging(akkaLogLevel), and Slf4J(slf4jLogLevel). The initial values of these are set in the configuration file as seen above. These can be overriden in the application.conf file.

These values can also be changed dynamically by calling methods**** on the LoggingSystem class.

## Log Structure
All messages are logged by default as Json. Logs can contain the following fields. These are listed in alphabetical order (this is the order displayed by the Json pretty printer).

* @componentName: The name of the component if present
* @host: The local host name
* @name: The name and version of the application being run
* @severity: The message level: trace, debug, info, warn, error or fatal
* actor: The path for an actor when using ActorLogging
* class: The class for ClassLogging or ActorLogging
* file: The file containing the log call
* kind: Either slf4j or akka. Not present for logger API
* line: The line where the message was logged
* message: The log message
* timestamp: The time the message was logged
* trace: Information for any exception specified in the logging call



@@@ note

* @host and @name will appear in log statements only if _fullHeaders_ is set as true in the configuration

@@@

## Create LoggingSystem

In order to start logging you need to start `LoggingSystem` as follows:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #create-logging-system }

Java
:   @@snip [JLocationServiceExampleClientApp.scala](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #create-logging-system }


## Enable logging

### Enable generic logging
To enable logging for some utility code that does not require `@componentName` in log statements you can inherit following traits

Scala
:   * For actor class extend `GenericLogger.Actor`
    * For non-actor class extend `GenericLogger.Simple`

Java
:   * For actor class inherit `JGenericLoggerActor`
    * For non-actor class inherit `JGenericLogger`

### Enable component level logging
Whereas, if you want to include `@componentName` in your log statements you need to first create an object/abstract class/interface as follows:

Scala
:   @@snip [ExampleLogger.scala](../../../../examples/src/main/scala/csw/services/commons/ExampleLogger.scala) { #component-logger }

Actor Java Class
:   @@snip [JExampleLogger.scala](../../../../examples/src/main/java/csw/services/commons/JExampleLoggerActor.java) { #jcomponent-logger-actor }

Non-Actor Java class
:   @@snip [JExampleLogger.scala](../../../../examples/src/main/java/csw/services/commons/JExampleLogger.java) { #jcomponent-logger }


Then, you need to inherit following object/interface

Scala
:   * For actor class extend `ExampleLogger.Actor`
    * For non-actor class extend `ExampleLogger.Simple`
    
Java
:   * For actor class inherit `JExampleLoggerActor`
    * For non-actor class inherit `JExampleLogger`


@@@ note { title="For java you need to get logger instance in your class " }

Java
:   @@snip [JLocationServiceExampleClient](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #get-java-logger }

@@@


## Log statements

A basic info statement can be written as follows:

Scala
:   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #log-info }

Java
:   @@snip [JLocationServiceExampleClient.scala](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #log-info }

The output of log statement will be:

```
{"@componentName":"examples",
 "@severity":"INFO",
 "actor":
   "akka.tcp://csw-examples-locationServiceClient@10.131.20.68:51135/user/$a",
 "class":"csw.services.location.LocationServiceExampleClient",
 "file":"LocationServiceExampleClientApp.scala",
 "line":119,
 "message":"Find result: None",
 "timestamp":"2017-06-28T17:17:34.853000000+05:30"
}

```

You can also use a `Map` in message as follows:

Scala
 :   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #log-info-map }
 
Java
 :   @@snip [JLocationServiceExampleClient.scala](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #log-info-map }

The output of log statement will be: 
 
```
{"@componentName":"examples",
 "@severity":"INFO",
 "actor":
   "akka.tcp://csw-examples-locationServiceClient@10.131.20.68:51135/user/$a",
 "class":"csw.services.location.LocationServiceExampleClient",
 "file":"LocationServiceExampleClientApp.scala",
 "line":111,
 "message":
   {"@msg":"Attempting to find connection",
    "exampleConnection":
      "AkkaConnection(ComponentId(LocationServiceExampleComponent,Assembly))"
   },
 "timestamp":"2017-06-28T17:17:34.848000000+05:30"
}

```
 
Also you can log an error with stacktrace as follows:
 
Scala
  :   @@snip [LocationServiceExampleClientApp.scala](../../../../examples/src/main/scala/csw/services/location/LocationServiceExampleClientApp.scala) { #log-error }
 
Java
 :   @@snip [JLocationServiceExampleClient.scala](../../../../examples/src/main/java/csw/services/location/JLocationServiceExampleClient.java) { #log-info-error }
   
  




