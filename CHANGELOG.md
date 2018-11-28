# Change Log

CSW Common Software is a reimplementation/refactoring of the prototype CSW code [here](https://github.com/tmtsoftware/csw-prototype) 
developed during the CSW design phase with changes to make the code and public APIs
more robust and resilient and to improve its usability and performance for use at the
TMT Observatory.

The product is in a new repository: [csw](https://github.com/tmtsoftware/csw).

All notable changes to this project will be documented in this file.

## [CSW v0.6.0] - 2018-11-28

This is version 0.6.0 the second release of the TMT Common Software for project stakeholders.  
This release includes csw test-kit, ordering guarantee in event publish api, enhancements to command service query api & bug fixes    
See [here](https://tmtsoftware.github.io/csw/0.6.0/) for a detailed documentation of this version of the CSW software.


## [CSW v0.6.0-RC3] - 2018-11-21

This is the release candidate 3 of the third release of the TMT Common Software for project stakeholders. 
This release includes updated command service documentation and fix for `query` to return `Started` once the command execution starts. 
See [here](https://tmtsoftware.github.io/csw/0.6.0-RC3/) for a detailed documentation of this version of the CSW software.

## [CSW v0.6.0-RC2] - 2018-11-15

This is the release candidate 2 of the third release of the TMT Common Software for project stakeholders. 
This release includes changes in command service and addition of testkit for CSW.
See [here](https://tmtsoftware.github.io/csw/0.6.0-RC2/) for a detailed documentation of this version of the CSW software.

## [CSW v0.6.0-RC1] - 2018-10-23

This is the release candidate 1 of the third release of the TMT Common Software for project stakeholders. 
This release includes event service.
See [here](https://tmtsoftware.github.io/csw/0.6.0-RC1/) for a detailed documentation of this version of the CSW software.

## [CSW v0.5.0] - 2018-08-31

This is version 0.5 the second release of the TMT Common Software for project stakeholders. 
This release includes Event Service.
See [here](https://tmtsoftware.github.io/csw/0.5.0/) for a detailed documentation of this version of the CSW software.

#### New Features
- Event Service
    - API and programming documentation, updated examples
    - Command Line Interface for testing
    - Updated giter8 template
#### Bug Fixes
- Prefix missing in Akka location (CSW-11)
- Protobuf serde fails for Java keys/parameters (DEOPSCSW-495)
#### Requests
- CurrentState missing StateName (CSW-1)
- CurrentState pubsub by StateName
#### Source Updates Needed
- Akka update to typed actors may require your source to be updated - if you have problems, use tmt-scw-programming slack channel for help
- Inclusion of Event Service requires an update to any Top Level Actor

#### Planned for the Next Release (Coming Soon...)
- Alarm Service including Examples, API and programming documentation

## [CSW v0.5.0-RC2] - 2018-08-24

This is the release candidate 2 of the second release of the TMT Common Software for project stakeholders. 
This release includes event service and partial implementation of alarm service.
See [here](https://tmtsoftware.github.io/csw/0.5.0-RC2/) for a detailed documentation of this version of the CSW software.

## [CSW v0.5.0-RC1] - 2018-08-01

This is the release candidate 1 of the second release of the TMT Common Software for project stakeholders. 
This release includes event service.
See [here](https://tmtsoftware.github.io/csw/0.5.0-RC1/) for a detailed documentation of this version of the CSW software.

## [CSW v0.4.0] - 2018-04-11

This is the first early release of the TMT Common Software for project stakeholders. 
See [here](https://tmtsoftware.github.io/csw/0.4.0/) for a detailed description of this version of the CSW software.

See the notes for [CSW v0.4.0-RC1] for release changes.


## [CSW v0.4.0-RC1] - 2018-04-04

This is the release candidate 1 of the first release of the TMT Common Software for project stakeholders. 
See [here](https://tmtsoftware.github.io/csw/0.4.0-RC1/) for a detailed description of this version of the CSW software.

### Changed

- Updated Location Service implementation is now based on Akka cluster for higher performance and proper operation in all required scenarios
- New Command Service APIs - FDR version was based on Akka messages
- Significant updates to Configuration Service implementation with some API changes
- New Logging APIs
- Updated to use latest Scala and Java versions and dependencies

### Planned for the Next Release (Coming Soon...)

- Event Service
- Alarm Service

## [CSW v0.3-FDR] - 2016-12-03

This prototype version was provided as part of the CSW Final Design Review 

### Added

- Added Java APIs and tests

- Added vslice and vsliceJava: detailed, vertical slice examples in Scala and Java

- Added AlarmService

- Added Scala and Java DSLs for working with configurations

- Added csw-services.sh startup script

### Changed

- Changed the APIs for HcdController, AssemblyController, Supervisor

- Changed APIs for working with configurations in Scala and Java

- Changed the Location Service APIs

- Updated all dependency versions, Akka version

- Changed APIs for Event and Telemetry Service


### Added
- Added [BlockingConfigManager](src/main/scala/csw/services/cs/core/BlockingConfigManager.scala) 
  (a blocking API to the Config Service)

- Added [PrefixedActorLogging](src/main/scala/csw/services/log/PrefixedActorLogging.scala) to use in place
  of ActorLogging, to include a component's subsystem and prefix in log messages (subsystem is part of a component's prefix)
  
- Added [HcdControllerClient](src/main/scala/csw/services/ccs/HcdControllerClient.scala) 
  and [AssemblyControllerClient](src/main/scala/csw/services/ccs/AssemblyControllerClient.scala) classes, as
  an alternative API that makes clear which methods can be call (or which messages can be sent to the actor)
  
- Add `get(path, date)` method to [ConfigManager](src/main/scala/csw/services/cs/core/ConfigManager.scala) and
  all Config Service APIs, so that you can get the version of a file for a given date

- Added new [Alarm Service](alarms) and [command line app](apps/asConsole).
  An Alarm Service [Java API](javacsw/README.alarms.md) is also available.

- Added a *Request* message to [AssemblyController](ccs/src/main/scala/csw/services/ccs/AssemblyController.scala) that
does something based on the contents of the configuration argument and returns a status and optional value (also a SetupConfig).
The main difference between Request and Submit is that Request can return a value, while Submit only returns a status.

- Added Java APIs for most services (See the [javacsw](javacsw) and  [util](util) subprojects)

### Changed
- Renamed the earlier Hornetq based `event` project to [event_old](event_old) 
  and renamed the Redis based `kvs` project to [events](events). 
  Classes with *KeyValueStore* in the name have been renamed to use *EventService*. 

- Renamed Config Service Java interfaces to start with I instead of J, to be more like the other Java APIs

- Reimplemented parts of the configuration classes, adding Scala and Java DSLs (See [util](util))

- Changed most log messages to debug level, rather than info

- Reimplemented the configuration classes, adding type-safe APIs for Scala and Java, JSON I/O, serialization (See [util](util))

- Changes the install.sh script to generate Scala and Java docs in the ../install/doc/{java,scala} directories

- Changed the [Configuration Service](cs) to use svn internally by default instead of git. In the svn implementation there
  is only one repository, rather than a local and a main repository..

- Reimplemented the [Command and Control Service](ccs) and [component packaging](pkg) classes:
  New HcdController, AssemblyController traits.
  No longer using the Redis based StateVariableStore to post state changes:
  The new version inherits a PublisherActor trait. You can subscribe to state/status messages from HCDs and
  assemblies.

- Changed the design of the [Location Service](loc) APIs.

## [CSW v0.2-PDR] - 2015-11-19

This prototype version was provided as part of the CSW Preliminary Design Review 
