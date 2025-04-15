# Change Log

CSW Common Software is a reimplementation/refactoring of the prototype CSW code [here](https://github.com/tmtsoftware/csw-prototype)
developed during the CSW design phase with changes to make the code and public APIs
more robust and resilient and to improve its usability and performance for use at the
TMT Observatory.

The product is in a new repository: [csw](https://github.com/tmtsoftware/csw).

All notable changes to this project will be documented in this file.

## Upcoming releases

## [CSW v6.0.0-RC3] - 2025-04-15

### Changes
- Upgraded all dependencies to latest versions
- Upgraded to Scala-3, JDK-21
- Replaced use of akka libraries with org.apache.pekko
- Added some supporting methods for Java for JSON serialization of the coordinate classes in csw.params.core.formats.JsonSupport
- Replaced embedded-redis lib with a different version that supports recent MacOS versions (for testing)

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/6.0.0-RC1/
- Scaladoc: https://tmtsoftware.github.io/csw/6.0.0-RC1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/6.0.0-RC1/api/java/index.html

## [CSW v5.0.1] - 2023-04-12
This is the final release version v5.0.1 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/5.0.1/) for a detailed documentation of this version of the CSW software.

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/5.0.1/
- Scaladoc: https://tmtsoftware.github.io/csw/5.0.1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/5.0.1/api/java/index.html

## [CSW v5.0.1-RC1] - 2023-03-24
This is a release candidate 1 for version 5.0.1 of the TMT Common Software.
See [here](https://tmtsoftware.github.io/csw/5.0.1-RC1/) for a detailed documentation of this version of the CSW software.

### Changes
- Fixed broken links in documentation.

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/5.0.1-RC1/
- Scaladoc: https://tmtsoftware.github.io/csw/5.0.1-RC1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/5.0.1-RC1/api/java/index.html

## [CSW v5.0.0] - 2022-11-14
This is final release v5.0.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/5.0.0/) for a detailed documentation of this version of the CSW software.

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/5.0.0/
- Scaladoc: https://tmtsoftware.github.io/csw/5.0.0/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/5.0.0/api/java/index.html

## [CSW v5.0.0-RC2] - 2022-10-06
This is a release candidate 2 for version 5.0.0 of the TMT Common Software.
See [here](https://tmtsoftware.github.io/csw/5.0.0-RC2/) for a detailed documentation of this version of the CSW software.


## [CSW v5.0.0-RC1] - 2022-09-14
This is a release candidate 1 for version 5.0.0 of the TMT Common Software.
See [here](https://tmtsoftware.github.io/csw/5.0.0-RC1/) for a detailed documentation of this version of the CSW software.

### Changes
- Upgrade the project to jdk 17 (Update the INSTALL.md)
- Added missing standard units
- [Breaking] ComponentInfo now takes componentHandlerClassName instead of behaviorFactoryClassName.
  Follow migration guide(https://tmtsoftware.github.io/csw/migration_guide/migration_guide_4.0.0_to_5.0.0/migration-guide-4.0.0-to-5.0.0.html)
- Starting Component in standalone or container mode is automatically derived based on config structure. So,
    - When using ContainerCmd App, used don't need to provide `--standalone` option anymore.
- Added `OffsetStart`, `OffsetEnd`, `InputRequestStart` & `InputRequestEnd` in Sequencer Observe Events.

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/5.0.0-RC1/
- Scaladoc: https://tmtsoftware.github.io/csw/5.0.0-RC1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/5.0.0-RC1/api/java/index.html

## [CSW v4.0.1] - 2022-02-09
This is final release v4.0.1 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/4.0.1/) for a detailed documentation of this version of the CSW software.

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/4.0.1/
- Scaladoc: https://tmtsoftware.github.io/csw/4.0.1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/4.0.1/api/java/index.html

## [CSW v4.0.1-RC1] - 2022-01-27
This is a release candidate 1 for version 4.0.1 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/4.0.1-RC1/) for a detailed documentation of this version of the CSW software.

### Changes
- Add `NOT_READY` choice to OperationalState
- Add `coaddsInExposure` and `coaddsDone` params in `OpticalDetectorExposureData` Observe event

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/csw/4.0.1-RC1/
- Scaladoc: https://tmtsoftware.github.io/csw/4.0.1-RC1/api/scala/index.html
- Javadoc: https://tmtsoftware.github.io/csw/4.0.1-RC1/api/java/index.html

## [CSW v4.0.0] - 2021-09-23

This is final release v4.0.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/4.0.0/) for a detailed documentation of this version of the CSW software.

### Changes

- Added migration guide for v3.0.0 -> v4.0.0

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/4.0.0/>
- Scaladoc: <https://tmtsoftware.github.io/csw/4.0.0/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/4.0.0/api/java/index.html>

## [CSW v4.0.0-RC2] - 2021-09-09

This is a release candidate 2 for version 4.0.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/4.0.0-RC2/) for a detailed documentation of this version of the CSW software.

### Changes

- Added logic to read MiniCrm configuration from config file and add defaults in `reference.conf` of project
- Added helper `DatabaseTestKit` with embedded postgres in `FrameworkTestKit`
- Added doc in technical section for Adding New Unit

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/4.0.0-RC2/>
- Scaladoc: <https://tmtsoftware.github.io/csw/4.0.0-RC2/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/4.0.0-RC2/api/java/index.html>

## [CSW v4.0.0-RC1] - 2021-08-20

This is a release candidate 1 for version 4.0.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/4.0.0-RC1/) for a detailed documentation of this version of the CSW software.

### Changes

- Added `JDefaultComponentHandlers` and `DefaultComponentHandlers` - default component handlers to create assembly or hcd in both scala and java
- Removed `allTags` and `allTagNames` method from `Coords` and `JCoords`
- Updated app name's with `csw` prefix according to `apps.prod.json` in `osw-apps` repo.
- Added UTC & TAI entries in units list.
- Changed default unit for `UTCTimeKey` & `TAITimeKey` from `second` to `utc` & `tai` respectively.
- Added new `WrongCommandTypeIssue` entry in `CommandIssue`.
- Removed `RaDec` from `KeyType`. Alternatively user can use `Coord` special KeyType to capture original use cases for RaDec.
- Removed `Struct` from `Key`. It was mentioned in the earlier release that `Struct` will be removed.
- Added ObserveEvent Factories for IRDetector, OpticalDetector, WFSDetector & Sequencer.
- Subsystem list got updated as per <https://docushare.tmt.org/docushare/dsweb/Services/Document-4780>.
- Added `initAlarms` & `getCurrentSeverity` methods in AlarmTestKit.

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/4.0.0-RC1/>
- Scaladoc: <https://tmtsoftware.github.io/csw/4.0.0-RC1/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/4.0.0-RC1/api/java/index.html>

## [CSW v3.0.1] - 2021-01-28

This is patch release over v3.0.0 of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/3.0.1/) for a detailed documentation of this version of the CSW software.

### Changes

- Added migration guide for v2.0 -> v3.0

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/3.0.1/>
- Scaladoc: <https://tmtsoftware.github.io/csw/3.0.1/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/3.0.1/api/java/index.html>

## [CSW v3.0.0] - 2021-01-22

This is the third major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/3.0.0/) for a detailed documentation of this version of the CSW software.

### Changes

- `->` method on a Key now takes a single parameter instead of varargs. For varargs, please use `set` method.<sup>[1](#3-0-0-1)</sup>
- `->` method on a Key that took an array of values has been removed. Please use `setAll` method instead.<sup>[1](#3-0-0-1)</sup>
- Removed usage of client-roles in favor of realm-roles in location server and config server HTTP routes.<sup>[1](#3-0-0-1)</sup>
- Contract change for Location Service API for `registration` and `location` models to incorporate metadata.
  `Metadata` is additional information associated with `registration`.<sup>[1](#3-0-0-1)</sup>
- Removed `RegistrationFactory` from `location-server` module. Instead, the following should be used by Scala and Java users to instantiate `AkkaRegistration`<sup>[1](#3-0-0-1)</sup>
  - For Scala, use `AkkaRegistrationFactory`.  It has an API change to expect an `actorRef` instead of the URI of `actorRef`
  - For Java, use the new`JAkkaRegistrationFactory`.
- Contract change for ComponentHandlers `initialize` and `onShutdown` methods, where the return type was changed from `Future[Unit]` to `Unit` i.e. from non-blocking to blocking.<sup>[1](#3-0-0-1)</sup>
- Changed the installation of `csw-apps`. The `coursier` program to be used to install applications instead of downloading apps from release page.<sup>[2](#3.0.0-2)</sup>
- `logging-aggregator-<some-version>.zip` will be available on the release page.<sup>[2](#3-0-0-2)</sup>
- Added new restrictions on Parameter Key naming. It cannot have `[`, `]` or `/` characters in the key name.<sup>[2](#3-0-0-2)</sup>
- Changed naming convention for network interface names from  `Public` and `Private` to `Outside` and `Inside` respectively.<sup>[2](#3-0-0-2)</sup>
- Minor fixes in STIL pipeline<sup>[3](#3-0-0-3)</sup>
- Ensured test report is generated for multi-jvm tests<sup>[4](#3-0-0-4)</sup>
- Fixed incorrect story id label in test<sup>[4](#3-0-0-4)</sup>
- Removed obsolete requirement linkage for DEOPSCSW-205<sup>[4](#3-0-0-4)</sup>
- Added support for test story report generation in multi jvm test plugin<sup>[5](#3-0-0-5)</sup>

### Version Upgrades

- Scala version upgrade to 2.13.3
- SBT version upgrade to 1.4.2
- Borer version upgrade to 1.6.2
- Akka version upgrade 2.6.10
- Akka-http version upgrade 10.2.1
- Keycloak version upgrade 11.0.2
- Lettuce version upgrade 6.0.1.RELEASE

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/3.0.0/>
- Scaladoc: <https://tmtsoftware.github.io/csw/3.0.0/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/3.0.0/api/java/index.html>

### Supporting Releases

<a name="3-0-0-1"></a>1: [CSW v3.0.0-M1](https://github.com/tmtsoftware/csw/releases/tag/v3.0.0-M1) - 2020-11-10<br>
<a name="3-0-0-2"></a>2: [CSW v3.0.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v3.0.0-RC1) - 2020-12-08<br>
<a name="3-0-0-3"></a>3: [CSW v3.0.0-RC2](https://github.com/tmtsoftware/csw/releases/tag/v3.0.0-RC2) - 2020-09-24<br>
<a name="3-0-0-4"></a>4: [CSW v3.0.0-RC3](https://github.com/tmtsoftware/csw/releases/tag/v3.0.0-RC3) - 2020-12-19<br>
<a name="3-0-0-5"></a>5: [CSW v3.0.0-RC4](https://github.com/tmtsoftware/csw/releases/tag/v3.0.0-RC4) - 2020-12-23

## [CSW v2.0.1] - 2020-03-20

This is a First minor release post Second major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.1/) for a detailed documentation of this version of the CSW software.

### Changes

- Updated giter8 template

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/2.0.1/>
- Scaladoc: <https://tmtsoftware.github.io/csw/2.0.1/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/2.0.1/api/java/index.html>

## [CSW v2.0.0] - 2020-03-19

This is the Second major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/2.0.0/) for a detailed documentation of this version of the CSW software.
Migration guide for v2.0.0 can be found [here](https://tmtsoftware.github.io/csw/2.0.0/migration_guide/migration-guides.html).

### Changes

- Simplified CommandResponseManager and removed auto-completion of commands<sup>[1](#2-0-0-1)</sup>
- Prefix has Subsystem in constructor<sup>[1](#2-0-0-1)</sup>
- Log statements have subsystem and prefix along with componentName<sup>[1](#2-0-0-1)</sup>
- AlarmKey and ComponentKey is constructed from prefix instead of string<sup>[1](#2-0-0-1)</sup>
- TcpLocation and HttpLocation has prefix along with AkkaLocation<sup>[1](#2-0-0-1)</sup>
- ComponentType is displayed to snake_case from lowercase<sup>[1](#2-0-0-1)</sup>
- Subsystem is displayed in uppercase instead of lowercase<sup>[1](#2-0-0-1)</sup>
- ArrayData and MatrixData does not require classtag for creation<sup>[1](#2-0-0-1)</sup>
- Admin routes for setting log level and getting log level are now available via gateway<sup>[1](#2-0-0-1)</sup>
- JSON contracts for location and command service added in paradox documentation<sup>[1](#2-0-0-1)</sup>
- Internal implementation of csw-services.sh script has changed. It is now based on Coursier and newly created `csw-services` sbt module.
To start all the CSW services, run `csw-services.sh start` command.
`csw-services.sh` runs all services in the foreground, pressing `ctr+c` will stop all the services.<sup>[2](#2-0-0-2)</sup>

### Version Upgrades

- Scala version upgrade to 2.13.1
- SBT version upgrade to 1.3.7
- Akka version upgrade to 2.6.3
- Kafka version upgrade to 2.4.0
- Borer version upgrade to 1.4.0

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/2.0.0/>
- Scaladoc: <https://tmtsoftware.github.io/csw/2.0.0/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/2.0.0/api/java/index.html>

### Supporting Releases

<a name="2-0-0-1"></a>1: [CSW v2.0.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v2.0.0-RC1) - 2020-02-06<br>
<a name="2-0-0-2"></a>2: [CSW v2.0.0-RC2](https://github.com/tmtsoftware/csw/releases/tag/v2.0.0-RC2) - 2020-02-26<br>
<a name="2-0-0-3"></a>3: [CSW v2.0.0-RC3](https://github.com/tmtsoftware/csw/releases/tag/v2.0.0-RC3) - 2020-03-03<br>

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

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/1.1.0-RC1/>
- Scaladoc: <https://tmtsoftware.github.io/csw/1.1.0-RC1/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/1.1.0-RC1/api/java/index.html>

## [CSW v1.0.0] - 2019-08-30

This is the first major release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/1.0.0/) for a detailed documentation of this version of the CSW software.

### Changes

- Replaced Kryo serialization with Borer-CBOR for Akka actor messages<sup>[1](#1-0-0-1)</sup>
- Replaced Play-JSON with Borer-JSON in Location service, Configuration Service and Admin Service<sup>[1](#1-0-0-1)</sup>
- Made Location, Config, Logging and Alarm service models to be cross compilable for ScalaJs<sup>[2](#1-0-0-2)</sup>
- Removed `BAD` and `TEST` subsystems<sup>[1](#1-0-0-1)</sup>
- Added SequencerCommandService and docs for it<sup>[1](#1-0-0-1)</sup>
- Separated Command service docs technical from Framework docs<sup>[3](#1-0-0-3)</sup>

### Api changes

- CommandService
  - `submit` now returns its initial response (e.g. `Started`) instead of waiting for the final response<sup>[2](#1-0-0-2)</sup>
  - Added `submitAndWait` which will submit the command and wait for its final response<sup>[2](#1-0-0-2)</sup>
  - Rename `submitAll` to `submitAllAndWait` in Command service as it waits for final response of all commands<sup>[1](#1-0-0-1)</sup>
- `Prefix` creation will throw `NoSuchElementException` if invalid subsystem is provided<sup>[1](#1-0-0-1)</sup>
- Replaced `ActorRef` with ActorRef `URI` in `AkkaRegistration`<sup>[2](#1-0-0-2)</sup>

### Version Upgrades

- Scala version upgrade to 2.13.0

### Documentation

- Reference paradox documentation: <https://tmtsoftware.github.io/csw/1.0.0/>
- Scaladoc: <https://tmtsoftware.github.io/csw/1.0.0/api/scala/index.html>
- Javadoc: <https://tmtsoftware.github.io/csw/1.0.0/api/java/index.html>

### Supporting Releases

<a name="1-0-0-1"></a>1: [CSW v1.0.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v1.0.0-RC1) - 2019-08-07<br>
<a name="1-0-0-2"></a>2: [CSW v1.0.0-RC2](https://github.com/tmtsoftware/csw/releases/tag/v1.0.0-RC2) - 2019-08-12<br>
<a name="1-0-0-3"></a>3: [CSW v1.0.0-RC3](https://github.com/tmtsoftware/csw/releases/tag/v1.0.0-RC3) - 2019-08-27<br>
<a name="1-0-0-4"></a>4: [CSW v1.0.0-RC4](https://github.com/tmtsoftware/csw/releases/tag/v1.0.0-RC4) - 2019-08-28<br>

## [CSW v0.7.0] - 2019-06-19

This is the fourth release of the TMT Common Software for project stakeholders.
This release includes Time Service, Authentication and Authorization Service, Database Service and Logging Aggregator Service.
See [here](https://tmtsoftware.github.io/csw/0.7.0/) for a detailed documentation of this version of the CSW software.

#### New Features

- **Time Service:** Provides APIs to access time in different timescales (UTC and TAI) with up to nano-second precision.
 Also provides scheduling APIs.<sup>[1](#0-7-0-1)</sup>
- **Authentication and Authorization Service:** Suite of libraries/adapters provided to help build an ecosystem of
 client & server side applications that enforce authentication & authorization policies for TMT<sup>[1](#0-7-0-1)</sup>
- **Database Service:** Provides a TMT-standard relational database and connection library<sup>[1](#0-7-0-1)</sup>
- **Logging Aggregator Service:** Provides recommendation and configurations for aggregating logs from TMT applications
 written in Scala, java, Python, C, C++, system logs, Redis logs, Postgres logs, Elasticsearch logs, Keycloak logs
 for developer and production setup.<sup>[1](#0-7-0-1)</sup>
- Replaced Protobuf serialisation by CBOR
- Added Technical documentation for all the services
- Support Unlocking of a component by Admin<sup>[1](#0-7-0-1)</sup>
- Added authentication and authorization to config service admin rest endpoints<sup>[1](#0-7-0-1)</sup>
- Integration of time service with event service and alarm service.<sup>[1](#0-7-0-1)</sup>
- Added new APIs to `EventPublisher` allowing to provide `startTime` in `eventGenerator` APIs<sup>[1](#0-7-0-1)</sup>
- Changed `EventPublisher` APIs with `eventGenerator` to allow optional publishing of events<sup>[1](#0-7-0-1)</sup>

#### Version Upgrades

- Migration to AdoptOpenJDK 11
- Akka version upgrade to 2.5.23

#### Bug Fixes

- Get route of config server with path for empty config file gives 404 instead of 200 (DEOPSCSW-626)<sup>[1](#0-7-0-1)</sup>

### Supporting Releases

<a name="0-7-0-1"></a>1: [CSW v0.7.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v0.7.0-RC1) - 2019-03-25<br>

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

  - `ComponentHandlers` (Implementation classes for HCDs and Assemblies) now receive a single
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

### Supporting Releases

<a name="0-6-0-1"></a>1: [CSW v0.6.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v0.6.0-RC1) - 2018-10-23<br>
<a name="0-6-0-2"></a>2: [CSW v0.6.0-RC2](https://github.com/tmtsoftware/csw/releases/tag/v0.6.0-RC2) - 2018-11-15<br>
<a name="0-6-0-3"></a>3: [CSW v0.6.0-RC3](https://github.com/tmtsoftware/csw/releases/tag/v0.6.0-RC3) - 2018-11-21<br>

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

### Supporting Releases

<a name="0-5-0-1"></a>1: [CSW v0.5.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v0.5.0-RC1) - 2018-08-01<br>
<a name="0-5-0-2"></a>2: [CSW v0.5.0-RC2](https://github.com/tmtsoftware/csw/releases/tag/v0.5.0-RC2) - 2018-08-24<br>

## [CSW v0.4.0] - 2018-04-11

This is the first early release of the TMT Common Software for project stakeholders.
See [here](https://tmtsoftware.github.io/csw/0.4.0/) for a detailed description of this version of the CSW software.

### Changed

- Updated Location Service implementation is now based on Akka cluster for higher performance and proper operation in all required scenarios
- New Command Service APIs - FDR version was based on Akka messages
- Significant updates to Configuration Service implementation with some API changes
- New Logging APIs
- Updated to use latest Scala and Java versions and dependencies

### Planned for the Next Release (Coming Soon...)

- Event Service
- Alarm Service

### Supporting Releases

<a name="0-4-0-1"></a>1: [CSW v0.4.0-RC1](https://github.com/tmtsoftware/csw/releases/tag/v0.4.0-RC1) - 2018-04-04<br>

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
