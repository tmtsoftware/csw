# TMT Common Software (CSW)

@@@ index
 - [Getting Started](commons/getting-started.md)
 - [Creating a Component](commons/create-component.md)
 - [Working with Multiple Components](commons/multiple-components.md)
 - [Using Alarms](commons/using-alarms.md)
 - [Adding Unit Tests](commons/unit-tests.md)
 - [Params](commons/params.md)
 - [Framework](commons/framework.md)
 - [Commands](commons/command.md)
 - [Logging Aggregator](commons/logging_aggregator.md)
 - [Services](commons/services.md)
 - [Applications](commons/apps.md)
 - [Deployment](commons/deployment.md)
 - [Testing](commons/testing.md)
 - [sbt tasks](commons/sbt-tasks.md)
 - [Manuals](commons/manuals.md)
 - [Migration Guides](migration_guide/migration-guides.md)
 - [CSW Service Contract](commons/contract.md)
 - [Technical Documentation](technical/technical.md)
@@@

Common Software is the package of services and infrastructure software that integrates the TMT software systems.

Visit [TMT website](https://www.tmt.org) to know more about Thirty Meter Telescope.

## Common Software Architecture

CSW is designed to support the Observing Mode-Oriented Architecture (OMOA). An observing mode is a well-defined 
instrument or engineering observing task and an associated set of owned resources, procedures, and capabilities that 
implement the mode. 
An example instrument observing mode is: IRIS multi-filter integral field spectroscopy using the NFIRAOS adaptive optics unit 
with AO laser guide star correction. An instrument will generally have several associated observing modes 
for acquisition, science objects, and calibrations. Examples of observing mode resources could be an instrument’s hardware 
devices, or the use of a larger system such as the Laser Guide Star Facility.

OMOA structures the software in layers as shown in the following figure. Each layer contains components with specific responsibilities 
described in the following sections. OMOA bypasses the use of standalone “subsystems” (large principal systems) 
for a flatter system that requires less code and allows the software system for an observing mode to optionally 
be more flexibly composed at run-time.

![layers](./images/top/OMOALayers2.png)

#### Layer 0 - Obseratory Hardware
Layer 0 represents the actual hardware being controlled and the hardware controllers that interface the hardware to the computer systems.
#### Layer 1 - Hardware Control Layer
The lowest layer in the OMOA software system, the Hardware Control Layer, consists of all the controllable hardware that is available 
for use by higher levels of software. A sea of similar software components called Hardware Control Daemons (HCD) at layer 1 
controls the low-level hardware of the telescope, adaptive optics, and instruments.

An HCD is similar to the device driver found in many systems. Each HCD is associated with a networked motion controller, a PLC/PAC, 
or other low-level hardware controller present in Layer 0. Some hardware controllers will support multiple channels. 
An HCD may support a highly cohesive, related set of functionality. 
For instance, one motion controller with 8 axes might handle all the slow moving filters and gratings of an instrument. 
In other cases, the channels of the controller hardware could be associated with unrelated devices. 
If the hardware controller has multiple channels, the HCD supports access to all the channels and must multiplex 
access to the controller and coordinate requests and replies among the clients.
#### Layer 2 - Assembly Layer
The Assembly Layer exists just above the Hardware Control Layer at Layer 2. Software at this layer consists 
of components called Assemblies.
In OMOA, an Assembly represents a device as a collection of hardware that makes sense at the user level. 
Examples of instrument devices are a filter wheel, a deformable mirror, or a detector controller. 
Assemblies often represent user-oriented devices in the software system, but it is not necessary that an Assembly control HCDs.
#### Layer 3 - Sequencing Layer
The Sequencing Layer is Layer 3 in the figure above. Components at this level are called Sequencers because 
they take complex descriptions of tasks and control and synchronize the actions of the Assemblies to accomplish the tasks. A
Sequence Component is a reusable OMOA application that can load a Script. Once the Sequence Component loads a Script, it 
becomes a Sequencer. This allows a Sequence Component to load a different Script based on the observing mode. 
Individual Sequencers and Scripts can provide a higher level of control for a set of distributed 
hardware (e.g., init) or can provide commands that are specific to an observing mode or engineering task.

Sequencers in this layer share a software interface that allows them to be plugged together to form a sequencing hierarchy 
for a specific observing mode. There can be one or many Sequencers in a hierarchy supporting a
specific observing mode. 
#### Layer 4 - Monitoring and Control Layer
The Monitoring/Control Layer is the layer of software that contains the user interface programs that are used to observe with the telescope. 
At TMT there will be graphical user interfaces for use by observers during observing. 
These applications use the CSW services to control and monitor the system.

## CSW Services
CSW or Common Software provides a shared software infrastructure based on a set of services and associated software for 
integrating individual components in the large observatory software architecture. The components and client applications use a set of 
loosely coupled services, each autonomous with a well-defined interface that hides the service implementation and also   
provides a TMT-standardized communication with the services.
  
### @ref:[Location Service](services/location.md)
The Location Service of TMT Common Software handles application, component, and service registration and discovery
in the distributed TMT software system. When a component (i.e. an Application, Sequencer, Assembly, HCD, Container, or Service) is 
initializing, it registers its name along with other information such as interface type and connection information to the
Location Service. The important feature and reason for the Location Service is that details of
connection information should not be hardwired, they should be discovered at runtime.

Location Service is most obviously needed when one component commands another
component. In this case the first component uses the Location Service to get information about
the second component, and uses that information to make a connection. Discovered information
might include a protocol (e.g., HTTP), interface type (e.g., command), or host and port.

### @ref:[Configuration Service](services/config.md)
The Configuration Service (CS) provides a centralized persistent store for “configuration files”
used in the TMT Software System. In this context, a configuration file is a set of values
describing state, initialization values, or other information useful to a component or set of
components. The TCS provides many examples such as look-up tables of various kinds or a set
of pointing model parameters or parameters for setting up a motion controller. Another is the
Alarm Service Configuration File. At the applications level, the GUI used by the
Observing Assistant could provide a button to save offsets between an instrument science field
and its acquisition camera origin. These are the kinds of scenarios that use the Configuration
Service.

The Configuration Service provides the added feature of storing versions of configuration files.
All versions of configuration files are retained providing a historical record of changes for each
configuration file. Components can save today’s version without fear that yesterday’s version will
be lost. If the configuration of a component is inadvertently lost, it will be possible to easily
restore to the most recently saved version or a default version.


### @ref:[Logging Service](services/logging.md)
Logging is the ability of a software component to output a message, usually for diagnostic
purposes. Common Software will provide a Logging Service. Logging should not be
confused with “data logging”, which is usually collection of measured values. 

This log message includes a time of the log message, a severity (INFO), the source of the log
message as a package path in the software, and a formatted text message.
The Logging Service provides the ability to log messages locally to a file or screen and optionally
to a centralized logging aggregator.

The central logging aggregator provides the capability for all components to log diagnostic
information to a central and optionally persistent store. The logging information is then
aggregated and ordered by timestamp. A coordinated, centralized log can be an extremely
useful tool for diagnosing many types of distributed software problems. Structured logging will be
used with the central logging aggregator.

The Logging Service is unique because it is required early in the lifecycle of a component and
most components and CSW services themselves will want the ability to log information. It is
often necessary to log messages while a component starts up. This means that the
implementation of distributed logging must not depend upon other services (at least if the
independence of services is desired). It also means that distributed logging will need to load
quickly and provide proper behavior if the aggregating logging capability is needed.

The logging API provides familiar features similar to available logging libraries including
logging levels and the ability to dynamically change the component’s logging configuration while
the component is executing. This allows the ability to interactively log more detailed messages when a component encounters problems.

### @ref:[Command Service](commons/command.md)
In the OMOA software design, an Application or Sequencer connects to Assemblies
and causes actions by submitting commands. Assemblies then connect to and command HCDs.
The service that provides the command functionality is called the Command
Service (CCS).

In the system design each observing mode has a Sequencer hierarchy that consists of one or more OMOA
Sequence Components/Sequencers. Commands flow down through the Sequencers to the
Assemblies, HCDs and hardware in a hierarchy.

In CSW, commands require peer-to-peer connections between the component sending a
command and the component receiving the command. There is no reason to directly connect to
a component unless that component will be commanded. The Location Service provides
connection information for components sending commands with CCS.

### @ref:[Event Service](services/event.md)
In an event-driven system, an event marks the occurrence of a state change, action, or activity
that is of interest in the system. In TMT many interactions between systems are best viewed as
being event-driven. For instance, Observe Events are used by a science detector to indicate
when activities have occurred such as closing the shutter at the conclusion of a science
observation. The TCS Pointing Assembly sends pointing demand events to mechanisms
throughout the software system.

The Event Service is based on the publish/subscribe messaging paradigm.
One component publishes an event and all components that have subscribed receive the event.
The advantage of this type of message system is that publishers and subscribers are decoupled.
Publishers can publish regardless of whether there are subscribers, and subscribers can
subscribe even if there are no publishers. The relationship between publishers and subscribers
can be one-to-one, one-to-many, many to one, or even many-to-many. Another advantage of 
publish-subscribe systems is that components and systems can startup and stop independently
without requiring special interactions or startup sequences with other systems.

The publish-subscribe pattern also allows the creation of event dependencies between systems
that are difficult to track but must be understood and managed. Dependencies can be passive or active. When a
component subscribes to a topic, but takes no action based on the value it is called a passive
dependency. For instance, a GUI display could subscribe to the current position of an instrument
filter but take no action other than displaying the value. An active dependency occurs when
a subscribing component uses the event value to alter its behavior. For instance, an atmospheric
dispersion corrector Assembly listens to the telescope zenith angle event in order to properly
correct for dispersion. 

### @ref:[Alarm Service](services/alarm.md)
An alarm is published to mark an abnormal condition that requires the attention of an operator or
other user. Alarms are not errors, they are conditions that occur asynchronously while
components are executing or inactive. For instance, an alarm could be published to indicate a
hardware limit. An example of this kind of alarm event is a detector temperature that is too high.
Alarms are most valuable to operators and observers who monitor the status of the telescope
systems and the instruments. The control GUIs will include standardized ways for displaying
alarms if needed. The Executive Software provides the observer and operator interfaces for the
purpose of displaying alarms relevant to observing from the instruments and telescope system.

However, **alarms are not a suitable or approved approach to hazard control as part of a
TMT safety system**. Nor should alarms be used to indicate errors. 
Alarms should provide additional information to operators and staff about
systems monitored by the OSS and can provide early warning of future hazardous conditions,
but should not be a sole, primary hazard control.

### @ref:[Time Service](services/time.md)
TMT has standardized on the use of [Precision Time Protocol (PTP)](https://en.wikipedia.org/wiki/Precision_Time_Protocol) 
as the basis of observatory time. The Time Service provides access to time based on the time provided by PTP. The Global
Positioning System (GPS) provides the absolute time base called Observatory Time. The PTP
grand master clock (a hardware device) is synchronized to Observatory Time. Each computer
system participating in the PTP system synchronizes to Observatory Time using the PTP
protocol. The time service also provides APIs for scheduling periodic and non-periodic tasks in the future, 
which are optimised for scheduling at up to 1KHz frequency. Time Service provides access to TAI and UTC time that
is synchronized to Observatory Time. Time Service also provides functions for scheduling tasks based on time.

### @ref:[Database Service](services/database.md)
The Database Service provides API to manage database connections and access data in the TMT software system. The service expects
`Postgres` as database server. It uses `Jooq` library underneath to manage database access, connection pooling, etc.
To describe `JOOQ` briefly, it is a Java library that provides an API for accessing data including DDL support, DML support, fetch,
batch execution, prepared statements, etc. safety against sql injection connection pooling, etc. To know more about JOOQ and
its features, please refer to this [link](https://www.jooq.org/learn/).

### @ref:[Framework](commons/framework.md)
The framework provides templates for @ref:[creating](./commons/create-component.md) and running the kind of software components 
defined by OMOA as well as service access interfaces for these components. It also provides @ref:[application](./apps/hostconfig.md) support
for running multiple components on a host machine.

The framework also contains the structures that are common to components, such as commands and event structures.

## CSW Roadmap
The 1.0.0 release was a crucial milestone of Common Software (CSW). All the required services of 
Common Software are included in the 1.0.0 release and subsequent releases. Release 1.0.0 is the 
primary deliverable of the CSW construction work package.
CSW is now in the maintenance phase. During the maintenance phase we expect twice per year maintenance releases of CSW.

Members of TMT work packages can add issues at the internal maintenance [site](/login.jsp?os_destination=%2Fsecure%2FRapidBoard.jspa%3FrapidView%3D57).    

**What to expect from releases after 1.0.0?**

- Upgrade to Scala 2.13
- Upgrade to Akka 2.6 
- Updates of dependencies
- Bug fixes and improvements based on user input
- Any changes needed to support ESW

## HTTP-based services API documentation
**Documentation for HTTP based services could be found [here](swagger/index.html).**

## CSW Javascript adapters documentation
**Documentation for CSW JS adapters could be found @extref[here](csw_js:)**
