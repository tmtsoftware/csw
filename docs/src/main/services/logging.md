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

## Using Log API


