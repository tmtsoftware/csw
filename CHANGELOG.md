# Change Log

CSW Common Software is a reimplementation/refactoring of the prototype CSW code [here](https://github.com/tmtsoftware/csw-prototype) 
developed during the CSW design phase with changes to make the code and public APIs
more robust and resilient and to improve its usability and performance for use at the
TMT Observatory.

The product is in a new repository: [csw](https://github.com/tmtsoftware/csw).

All notable changes to this project will be documented in this file.

## [CSW upcoming version]
- `->` method on a Key now takes a single parameter instead of varargs. For varargs, please use `set` method.
- `->` method on a Key that took an array of values has been removed. Please use `setAll` method instead.
- `->` Removed usage of client-role and used realm-role instead in location server and config server routes.
- Contract change for location service API for example `registration` and `location` model incorporate metadata.
  `Metadata` is additional information associated with `registration`.
- Removed `RegistrationFactory` from `location-server` module. Instead, following should be used by Scala and Java users to instantiate `AkkaRegistration`
    - For Scala users, `AkkaRegistrationFactory` API change to expect actorRef instead of URI of remote actorRef
    - For Java users, `JAkkaRegistrationFactory` is added.
- Contract change for ComponentHandlers `initialize` method, return type changed from `Future[Unit]` to `Unit` i.e. from non-blocking to blocking.  
- Changed the installation of csw-apps, coursier to be used to install applications instead of downloading apps.zip from release page.
- logging-aggregator-<some-version>.zip will be available on the release page. 

## [CSW v3.0.0-M1] - 2020-09-24
- `->` method on a Key now takes a single parameter instead of varargs. For varargs, please use `set` method.
- `->` method on a Key that took an array of values has been removed. Please use `setAll` method instead.
- `->` Removed usage of client-role and used realm-role instead in location server and config server routes.
- Contract change for location service API for example `registration` and `location` model incorporate metadata.
  `Metadata` is additional information associated with `registration`.
- Removed `RegistrationFactory` from `location-server` module. Instead, following should be used by Scala and Java users to instantiate `AkkaRegistration`
    - For Scala users, `AkkaRegistrationFactory` API change to expect actorRef instead of URI of remote actorRef
    - For Java users, `JAkkaRegistrationFactory` is added.
- Contract change for ComponentHandlers `initialize` method, return type changed from `Future[Unit]` to `Unit` i.e. from non-blocking to blocking.  

## [CSW v2.0.1] - 2020-03-20

This is a First minor release post Second major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.1/) for a detailed documentation of this version of the CSW software.

### Changes
- Updated giter8 template

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/2.0.1/
- Scaladoc: https://tmtsoftware.github.io/csw/2.0.1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/2.0.1/api/java/index.html


## [CSW v2.0.0] - 2020-03-19

This is the Second major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.0/) for a detailed documentation of this version of the CSW software.
Migration guide for v2.0.0 can be found [here](https://tmtsoftware.github.io/csw/2.0.0/migration_guide/migration-guides.html).

### Changes
- Simplified CommandResponseManager and removed auto-completion of commands
- Prefix has Subsystem in constructor 
- Log statements have subsystem and prefix along with componentName
- AlarmKey and ComponentKey is constructed from prefix instead of string
- TcpLocation and HttpLocation has prefix along with AkkaLocation
- ComponentType is displayed to snake_case from lowercase 
- Subsystem is displayed in uppercase instead of lowercase
- ArrayData and MatrixData does not require classtag for creation 
- Admin routes for setting log level and getting log level are now available via gateway
- JSON contracts for location and command service added in paradox documentation
- Internal implementation of csw-services.sh script has changed. It is now based on Coursier and newly created `csw-services` sbt module.
To start all the CSW services, run `csw-services.sh start` command.
`csw-services.sh` runs all services in the foreground, pressing `ctr+c` will stop all the services. 

### Version Upgrades
- Scala version upgrade to 2.13.1
- SBT version upgrade to 1.3.7
- Akka version upgrade to 2.6.3
- Kafka version upgrade to 2.4.0
- Borer version upgrade to 1.4.0

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/2.0.0/
- Scaladoc: https://tmtsoftware.github.io/csw/2.0.0/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/2.0.0/api/java/index.html


## [CSW v2.0.0-RC3] - 2020-03-03

This is the release candidate 3 for the second major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.0-RC3/) for a detailed documentation of this version of the CSW software.
Migration guide for v2.0.0-RC3 can be found [here](https://tmtsoftware.github.io/csw/2.0.0-RC3/migration_guide/migration-guides.html).

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/2.0.0-RC3/
- Scaladoc: https://tmtsoftware.github.io/csw/2.0.0-RC3/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/2.0.0-RC3/api/java/index.html

## [CSW v2.0.0-RC2] - 2020-02-26

This is the release candidate 2 for the second major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.0-RC2/) for a detailed documentation of this version of the CSW software.
Migration guide for v2.0.0-RC2 can be found [here](https://tmtsoftware.github.io/csw/2.0.0-RC2/migration_guide/migration-guides.html).

### Changes
Internal implementation of `csw-services.sh` script has changed. It is now based on Coursier and newly created `csw-services` sbt module.
To start all the CSW services, run `csw-services.sh start` command. 
`csw-services.sh` runs all services in the foreground, pressing `ctr+c` will stop all the services.

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/2.0.0-RC2/
- Scaladoc: https://tmtsoftware.github.io/csw/2.0.0-RC2/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/2.0.0-RC2/api/java/index.html

## [CSW v2.0.0-RC1] - 2020-02-06

This is the release candidate 1 for the release 2.0.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.0-RC1/) for a detailed documentation of this version of the CSW software.

### Changes
- Simplified CommandResponseManager and removed auto-completion of commands
- Prefix has Subsystem in constructor 
- Log statements have subsystem and prefix along with componentName
- AlarmKey and ComponentKey is constructed from prefix instead of string
- TcpLocation and HttpLocation has prefix along with AkkaLocation
- ComponentType is displayed to snake_case from lowercase 
- Subsystem is displayed in uppercase instead of lowercase
- ArrayData and MatrixData does not require classtag for creation 
- Admin routes for setting log level and getting log level are now available via gateway
- JSON contracts for location and command service added in paradox documentation

### Version Upgrades
- Scala version upgrade to 2.13.1
- SBT version upgrade to 1.3.7
- Akka version upgrade to 2.6.3
- Kafka version upgrade to 2.4.0
- Borer version upgrade to 1.4.0

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/2.0.0-RC1/
- Scaladoc: https://tmtsoftware.github.io/csw/2.0.0-RC1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/2.0.0-RC1/api/java/index.html

## [CSW v1.1.0-RC1] - 2020-02-04

This is the release candidate 1 for the release 1.1.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/1.1.0-RC1/) for a detailed documentation of this version of the CSW software.

### Changes
- Simplified CommandResponseManager and removed auto-completion of commands
- Prefix has Subsystem in constructor 
- Log statements have subsystem and prefix along with componentName
- AlarmKey and ComponentKey is constructed from prefix instead of string
- TcpLocation and HttpLocation has prefix along with AkkaLocation
- ComponentType is displayed to snake_case from lowercase 
- Subsystem is displayed in uppercase instead of lowercase
- ArrayData and MatrixData does not require classtag for creation 
- Admin routes for setting log level and getting log level are now available via gateway
- JSON contracts for location and command service added in paradox documentation

### Version Upgrades
- Scala version upgrade to 2.13.1
- SBT version upgrade to 1.3.7
- Akka version upgrade to 2.6.3
- Kafka version upgrade to 2.4.0
- Borer version upgrade to 1.4.0

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/1.1.0-RC1/
- Scaladoc: https://tmtsoftware.github.io/csw/1.1.0-RC1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/1.1.0-RC1/api/java/index.html


## [CSW v1.0.0] - 2019-08-30

This is the first major release of the TMT Common Software for project stakeholders. 
See [here](https://tmtsoftware.github.io/csw/1.0.0/) for a detailed documentation of this version of the CSW software.

### Changes
- Replaced Kryo serialization with Borer-CBOR for Akka actor messages
- Replaced Play-JSON with Borer-JSON in Location service, Configuration Service and Admin Service
- Made Location, Config, Logging and Alarm service models to be cross compilable for ScalaJs
- Removed `BAD` and `TEST` subsystems
- Added SequencerCommandService and docs for it
- Separated Command service docs technical from Framework docs

### Api changes
- CommandService
    - `submit` now returns its initial response (e.g. `Started`) instead of waiting for the final response 
    - Added `submitAndWait` which will submit the command and wait for its final response
    - Rename `submitAll` to `submitAllAndWait` in Command service as it waits for final response of all commands
- `Prefix` creation will throw `NoSuchElementException` if invalid subsystem is provided
- Replaced `ActorRef` with ActorRef `URI` in `AkkaRegistration`  

### Version Upgrades
- Scala version upgrade to 2.13.0

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/1.0.0/
- Scaladoc: https://tmtsoftware.github.io/csw/1.0.0/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/1.0.0/api/java/index.html

## [CSW v1.0.0-RC4] - 2019-08-28

This is the release candidate 4 for the first major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/1.0.0-RC4/) for a detailed documentation of this version of the CSW software.

## [CSW v1.0.0-RC3] - 2019-08-27

This is the release candidate 3 for the first major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/1.0.0-RC3/) for a detailed documentation of this version of the CSW software.

## [CSW v1.0.0-RC2] - 2019-08-12

This is the release candidate 2 for the first major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/1.0.0-RC2/) for a detailed documentation of this version of the CSW software.

### Changes
- Added `SequencerCommandService`
- Replaced Kryo serialization with Borer-CBOR for Akka actor messages
- Replaced Play-JSON with Borer-JSON in Location service
- Removed `BAD` and `TEST` subsystems
- Made Alarm, Config, Logging and Location service models to be cross compilable for ScalaJs

### Api changes
- Command Service
    - `submit` now returns its initial response (e.g. `Started`) instead of waiting for the final response 
    - Added `submitAndWait` which will submit the command and wait for its final response
    - Rename `submitAll` to `submitAllAndWait` in Command service as it waits for final response of all submitted commands

- `Prefix` creation will throw `NoSuchElementException` if invalid subsystem is provided
- Replaced `ActorRef` with ActorRef `URI` in `AkkaRegistration`

### Version Upgrades
- Scala version upgrade to 2.13.0

## [CSW v1.0.0-RC1] - 2019-08-07

This is the release candidate 1 for the first major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/1.0.0-RC1/) for a detailed documentation of this version of the CSW software.

### Changes
- Added `SequencerCommandService`
- Replaced Kryo serialization with Borer-CBOR for Akka actor messages
- Replaced Play-JSON with Borer-JSON in Location service
- Removed `BAD` and `TEST` subsystems

### Api changes
- Command Service
    - Rename `submit` api to `submitAndWait` in Command service as it waits for final response
    - Rename `submitAll` to `submitAllAndWait` in Command service as it waits for final response of all commands
    - Added `submit` api in Command service which will submit the command to component and return the SubmitResponse
 
- `Prefix` creation will throw `NoSuchElementException` if invalid subsystem is provided

### Version Upgrades
- Scala version upgrade to 2.13.0

## [CSW v0.7.0] - 2019-06-19

This is the fourth release of the TMT Common Software for project stakeholders. 
This release includes Time Service, Authentication and Authorization Service, Database Service and Logging Aggregator Service.
See [here](https://tmtsoftware.github.io/csw/0.7.0/) for a detailed documentation of this version of the CSW software.

#### New Features

- **Time Service:** Provides APIs to access time in different timescales (UTC and TAI) with up to nano-second precision.
 Also provides scheduling APIs. 
- **Authentication and Authorization Service:** Suite of libraries/adapters provided to help build an ecosystem of
 client & server side applications that enforce authentication & authorization policies for TMT
- **Database Service:** Provides a TMT-standard relational database and connection library
- **Logging Aggregator Service:** Provides recommendation and configurations for aggregating logs from TMT applications
 written in Scala, java, Python, C, C++, system logs, Redis logs, Postgres logs, Elasticsearch logs, Keycloak logs 
 for developer and production setup.
- Replaced Protobuf serialisation by CBOR
- Added Technical documentation for all the services
- Support Unlocking of a component by Admin
- Added authentication and authorization to config service admin rest endpoints
- Integration of time service with event service and alarm service.
- Added new APIs to `EventPublisher` allowing to provide `startTime` in `eventGenerator` APIs 
- Changed `EventPublisher` APIs with `eventGenerator` to allow optional publishing of events

#### Version Upgrades
- Migration to AdoptOpenJDK 11
- Akka version upgrade to 2.5.23

#### Bug Fixes
- Get route of config server with path for empty config file gives 404 instead of 200 (DEOPSCSW-626)

## [CSW v0.7.0-RC1] - 2019-03-25

This is the release candidate 1 of the fourth release of the TMT Common Software for project stakeholders. 
This release includes Time Service, Authentication and Authorization Service, Database Service and Logging Aggregator Service.
See [here](https://tmtsoftware.github.io/csw/0.7.0-RC1/) for a detailed documentation of this version of the CSW software.

#### New Features

- **Time Service:** Provides APIs to access time in different timescales (UTC and TAI) with up to nano-second precision.
 Also provides scheduling APIs. 
- **Authentication and Authorization Service:** Suite of libraries/adapters provided to help build an ecosystem of
 client & server side applications that enforce authentication & authorization policies for TMT
- **Database Service:** Provides a TMT-standard relational database and connection library
- **Logging Aggregator Service:** Provides recommendation and configurations for aggregating logs from TMT applications
 written in Scala, java, Python, C, C++, system logs, Redis logs, Postgres logs, Elasticsearch logs, Keycloak logs 
 for developer and production setup.
- Support Unlocking of a component by Admin
- Added authentication and authorization to config service admin rest endpoints
- Integration of time service with event service and alarm service.
- Added new APIs to `EventPublisher` allowing to provide `startTime` in `eventGenerator` APIs 
- Changed `EventPublisher` APIs with `eventGenerator` to allow optional publishing of events

#### Version Upgrades
- Migration to AdoptOpenJDK 11
- Akka version upgrade to 2.5.21

#### Bug Fixes
- Get route of config server with path for empty config file gives 404 instead of 200 (DEOPSCSW-626)

## [CSW v0.6.0] - 2018-11-28

This is the version 0.6.0 release of the TMT Common Software for project stakeholders.  
This release includes csw test-kit, ordering guarantee in event publish api, enhancements to command service query api and bug fixes.
  
See [here](https://tmtsoftware.github.io/csw/0.6.0/) for a detailed documentation of this version of the CSW software. See also the csw [release](https://github.com/tmtsoftware/csw/releases) page.

#### Changes

- GitHub Repository Renamed

    - The GitHub repository has been renamed from `csw-prod` to [csw](https://github.com/tmtsoftware/csw), while the old csw repository was renamed to `csw-prototype`.

#### API Changes

- Package Name Changes

    - The top level package in all modules has changed from `csw.services` to `csw`.
    
- Command Service

    -  `ComponentHandlers` (Implementation classes for HCDs and Assemblies) now receive a single
       `CswContext` object containing references to all the CSW services (Previously the services were passed as separate arguments).
    
    - `CommandService.submit()` now returns a future with the final response.
        Previously it returned the initial response, which could be `Accepted` for a long running command. 
        In csw-0.6.0 you can call `CommandService.query()` to get an initial `Started` response, if needed, after the call to `submit()`.
    
    - `CommandService.query()` now waits for the initial command response, if it has not yet been received. Previously it returned `CommandNotAvailable` if called too early.
    
    - There are now separate response types for submit, validation and query (Previously all used `CommandResponse`). A long running submit command now responds with `Started` (was `Accepted`).
    
    - The API for `ComponentHandlers` has changed. Now `onSubmit()` returns either `Started` for a 
      long running command that will complete later, or it can complete the command immediately and return the response, such as `Completed` or `Error`.
    
    - The `csw-messages` dependency was renamed to `csw-params` and is now compiled for both
      the JVM and ScalaJS.
          
- Location Service

    - It is no longer necessary for components (HCDs, assemblies) and applications to join the
      location service cluster and no need to define any environment variables or system properties
      before starting the components or applications (Previously `CLUSTER_SEEDS` and `INTERFACE_NAME` had to be defined). 
      
    - Location service access is now via an HTTP server running on each host 
    (The HTTP servers form a cluster).
    
    - Command line applications now use the HTTP API for location service, resulting in much 
    faster startup times.

- Event Service

    - The event service has been updated to make sure events are published in order, even if the caller does not wait for the returned future to complete before publishing again.

#### New Features

- Alarm Service

    - The Alarm Service API is now complete.

- New Template for Component Builders

    - Updated giter8 template. You can create a new HCD or assembly project with `sbt new tmtsoftware/csw.g8`.
    
- Test Kits

    - Test kits are now available that can start and stop CSW services inside tests so that
      there is no need to run csw-services.sh before running the tests.
      See `ScalaTestFrameworkTestKit` and `FrameworkTestKit` (for Java).


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
