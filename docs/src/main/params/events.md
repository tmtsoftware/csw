# Events

Events are the most basic type of asynchronous notification in TMT when an activity occurs
somewhere in the TMT system and other components need to be notified. Each type of event has a unique
purpose and unique information, but they all share same structural features. 
All events have **EventInfo** and a **ParameterSet**.

@@@ note

The `csw-params` library offers out of the box support to serialize Events using **Cbor**, so that events can be produced and
consumed by JVM (Java virtual machine) as well as Non-JVM applications.

For more on Cbor, refer to the @ref:[technical doc](../technical/params/params.md).

@@@

## EventTime

Each event includes its time of creation in UTC format. You can access that eventTime as follows:

Scala
: @@snip [EventsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #eventtime }

Java
: @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #eventtime }

## System Event

`SystemEvent` is the type used to describe the majority of events in the system. An example is a demand that is
the output of an algorithm in one component that is used as an input to another. `SystemEvent` is also used
to publish internal state or status values of a component that may be of interest to other components in the system.

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #systemevent }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #systemevent }

## Observe Event

ObserveEvent are standardized events used to describe an activities within the data acquisition process.
These events are typically published by Science Detector Assemblies, which emit ObserveEvents during their exposures
to signal the occurrence of specific activities/actions during the acquisition of data.
Observe Events serve the following purposes in OSW:

- Provide a TMT-standardized API for notification of activities in detector systems 
- Provide TMT-standardized events that can be used to synchronize and trigger actions in other parts of the distributed software system 
- Provide  a  standardized API  to  science  and  engineering  detector  systems  to  ease  interfacing Sequencers 
- Provide TMT-standardized APIs that can be more easily used by user interface programs that must deal with many detector systems
- Provide notification of actions that can be used to generate observatory efficiency metrics and time accounting

### IR Science Detector Systems Observe Events

The Observe Events for IR science detector systems are based on the events proposed for the IRIS  instrument  at  their  PDR2.  These  events  include  Observe  Events  for  actions.  
A  state  and data  event  are defined  to  allow  user  interfaces  and  Sequencers  to  monitor  and  control  with similar behavior across the observatory.

| Observe Event  | Description                                                                                                                                                   | Probable Publisher                                             |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- |
| observeStart   | Indicates the start of execution of actions  related to an Observe command                                                                                    | Instrument Sequencer or  possibly OCS Sequencer                |
| exposureStart  | Indicates the start of data acquisition that  results in a file produced for DMS. This is a potential metadata event for DMS                                  | Detector Assembly or  possibly Instrument            Sequencer |
| exposureEnd    | Indicates the end of data acquisition that results  in a file produced for DMS. This is a potential metadata event for DMS                                    | Detector Assembly or  possibly Instrument Sequencer            |
| readoutEnd     | Indicates that a readout that is part of a ramp  has completed                                                                                                | Detector HCD or possibly  Detector Assembly                    |
| readoutFailed  | Indicates that a readout that is part of a ramp  has failed indicating transfer failure or some  other issue                                                  | Detector HCD or possibly  Detector Assembly                    |
| dataWriteStart | Indicates that the instrument has started writing  the exposure data file or transfer of exposure  data to DMS.                                               | Detector Assembly or  possibly Detector HCD                    |
| dataWriteEnd   | Indicates that the instrument has finished  writing the exposure data file or transfer of  exposure data to DMS.                                              | Detector Assembly or possibly Detector HCD                     |
| exposureAbort  | Indicates that a request was made to abort the  exposure and it has completed. Normal data events should occur if data is  recoverable. Abort should not fail | Detector Assembly or  possibly Detector HCD                    |
| observeEnd     | Indicates the end of execution of actions related  to an Observe command                                                                                      | Instrument Sequencer or  possibly OCS Sequencer                |

Two additional standardized events are defined to enable uniform detector information for  Sequencers and user interfaces. These events are also based on the IRIS design.  

#### IR Detector Exposure State Event

An exposure state Observe Event called irDetectorExposureState is provided as a state variable  to indicate the current state of the detector system. The Exposure State Event groups  parameters that change relatively slowly, and this event should be published whenever any of  the parameters changes.
The Exposure State Event includes the following mandatory parameters, but a  detector system may add additional parameters as well. (The parameters in this event are based  on the IRIS PDR2 model files.)

| Parameters         | Description                                                                                                                                                                                                                                                                                                                                                               |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| exposureId            | ExposureId is an identifier in ESW/DMS for a single exposure. The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time when the ExposureId is created. |
| exposureInProgress | String identification of detector system (should be a single phrase with  no spaces e.g., my_detector). A Boolean indicating if detector system is acquiring an exposure  • Delimited by exposureStart and exposureEnd. exposureInProgress should be false if abortInProgress is true (TBD)                                                                               |
| abortInProgress    | Indicates that an abort has been requested and is underway                                                                                                                                                                                                                                                                                                                |
| isAborted          | Indicates that an abort has occurred and is completed. abortInProgress should be false when isAborted is true. isAborted should be set to false with the next exposure                                                                                                                                                                                                    |
| operationalState   | Enumeration indicating if the detector system is available and  operational. READY, BUSY, ERROR.  READY indicates system can execute exposures. BUSY indicates system is BUSY most likely acquiring data. ERROR indicates the detector system is in an error state. This could  happen as a result of a command or a spontaneous failure. Corrective  action is required. |
| errorMessage       | An optional parameter that can be included when the detector system  is in the ERROR operationalState. This value should be cleared and removed from the state when the  operationalState returns to READY                                                                                                                                                                |

This event should be published whenever any of the state parameters change. Parameters such  as errorMessage and isAborted should be cleared when a new exposure is started. 

#### IR Detector Exposure Data Event

A second mandatory Observe Event named irDetectorExposureData contains fast-changing  data about the internals of the current exposure. This data is useful for user interfaces and  Sequencers. This event should be published at the read rate or 1 Hz (whichever is less) during  an ongoing exposure.

| Parameters            | Description                                                                                                                                                                                                                                                                                             |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| exposureId            | ExposureId is an identifier in ESW/DMS for a single exposure. The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time when the ExposureId is created. |
| readsInRamp           | The integer total number of reads in the ramp. Value should  be constant during an exposure. (Note: for multi-array  detectors, it is assumed that all arrays work with the same  configuration).                                                                                                       |
| readsComplete         | Integer number of current completed read from 1 to  readsInRamp. Should be reset to 0 at the start of every ramp                                                                                                                                                                                        |
| rampsInExposure       | The integer total number of ramps in the current exposure. Value should be constant during an exposure                                                                                                                                                                                                  |
| rampsComplete         | Integer number of completed ramp from 1 to rampsInExposure. Should be reset to 0 at the start of every exposure                                                                                                                                                                                         |
| exposureTime          | Length in seconds of the current exposure                                                                                                                                                                                                                                                               |
| remainingExposureTime | Number of seconds remaining in current exposure • Should count down in seconds – no faster than 1 Hz                                                                                                                                                                                                    |

### Optical Science Detector Systems

The first light CCD-based instrument WFOS is not as advanced as IRIS and information in this  section is based on author experience. The specific events may change in detail, but it is  expected that what is presented here is adequate.

Optical detectors are often larger and somewhat simpler to control at the electronics level than  IR detectors. Optical detector systems for astronomy often include several CCDs forming a  mosaic that together cover the focal plane and are viewed as a single image. All the detectors  operate in unison as a single detector system.  

Optical detectors need to have shutters to control when light is collected on the detector. CCD based detectors accumulate photons/charge while the shutter is open and are read out after the  shutter closes. Some modes such as nod and shuffle require more detailed control of the  detector and system. (This is not covered here because TMT has not mentioned any modes  such as this. The design may need to be extended to handle other ways of using the CCD.)

The optical science detector system Observe Events are largely identical to the IR science  detector Observe Events

| Observe Event  | Description                                                                                                                                                   | Probable Publisher                                  |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------- |
| observeStart   | Indicates the start of execution of actions  related to an Observe command                                                                                    | Instrument Sequencer or  possibly OCS Sequencer     |
| prepareStart   | Indicates that the detector system is preparing  to start an exposure                                                                                         | Detector Assembly or  possibly Instrument Sequencer |
| exposureStart  | Indicates the start of data acquisition that  results in a file produced for DMS. This is a potential metadata event for DMS                                  | Detector Assembly or  possibly Instrument Sequencer |
| exposureEnd    | Indicates the end of data acquisition that results  in a file produced for DMS. This is a potential metadata event for DMS                                    | Detector Assembly or  possibly Instrument Sequencer |
| readoutEnd     | Indicates that a readout that is part of a ramp  has completed                                                                                                | Detector HCD or possibly  Detector Assembly         |
| readoutFailed  | Indicates that a readout that is part of a ramp  has failed indicating transfer failure or some  other issue                                                  | Detector HCD or possibly  Detector Assembly         |
| dataWriteStart | Indicates that the instrument has started writing  the exposure data file or transfer of exposure  data to DMS.                                               | Detector Assembly or  possibly Detector HCD         |
| dataWriteEnd   | Indicates that the instrument has finished  writing the exposure data file or transfer of  exposure data to DMS.                                              | Detector Assembly or possibly Detector HCD          |
| exposureAbort  | Indicates that a request was made to abort the  exposure and it has completed. Normal data events should occur if data is  recoverable. Abort should not fail | Detector Assembly or  possibly Detector HCD         |
| observeEnd     | Indicates the end of execution of actions related  to an Observe command                                                                                      | Instrument Sequencer or  possibly OCS Sequencer     |

@@@note
there is a question about whether observeStart and observeEnd are generated by the  Instrument Sequencer or OCS Sequencer. Each observeStart and observeEnd is associated  with an Observe command. If an instrument sequencer never receives more than one Observe,  then the Observe Events can be published by the OCS Sequencer. 
Note that these events have required parameters, which are given in RD06
@@@

Two additional standardized events are defined to enable uniform detector information for  Sequencers and user interfaces. These events are also based on the preceding sections. 

#### Optical Detector Exposure State Event

An exposure state Observe Event called opticalDetectorExposureState is provided as a state  variable to indicate the current state of the detector system. The Exposure State Event groups  parameters that change relatively slowly, and this event should be published whenever any of its  parameters changes. 
The Exposure State Event includes the following mandatory parameters (Table 3-4), but a  detector system may add their own parameters as well. The optical and IR detector exposure  state events are identical. 

| Parameters         | Description                                                                                                                                                                                                                                                                                                                                                               |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| exposureId         | ExposureId is an identifier in ESW/DMS for a single exposure. The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time when the ExposureId is created.                                                                   |
| exposureInProgress | String identification of detector system (should be a single phrase with  no spaces e.g., my_detector). A Boolean indicating if detector system is acquiring an exposure  • Delimited by exposureStart and exposureEnd. exposureInProgress should be false if abortInProgress is true (TBD)                                                                               |
| abortInProgress    | Indicates that an abort has been requested and is underway                                                                                                                                                                                                                                                                                                                |
| isAborted          | Indicates that an abort has occurred and is completed. abortInProgress should be false when isAborted is true. isAborted should be set to false with the next exposure                                                                                                                                                                                                    |
| operationalState   | Enumeration indicating if the detector system is available and  operational. READY, BUSY, ERROR.  READY indicates system can execute exposures. BUSY indicates system is BUSY most likely acquiring data. ERROR indicates the detector system is in an error state. This could  happen as a result of a command or a spontaneous failure. Corrective  action is required. |
| errorMessage       | An optional parameter that can be included when the detector system  is in the ERROR operationalState. This value should be cleared and removed from the state when the  operationalState returns to READY                                                                                                                                                                |

This event is standardized to allow ESW user interfaces to have minimal, uniform information  about the state of any Science Detector System.

#### Optical Detector Exposure Data Event

A second mandatory Observe Event named opticalDetectorExposureData event contains faster changing data about the internals of the current exposure. This data is useful for user interfaces  and Sequencers. This event should be published at 1 Hz during an ongoing exposure. This  event does not have much information compared to the IR use case and is primarily for tracking  the remaining current exposure time in user interfaces or sequencers.

| Parameters            | Description                                                                                                                                                                                                                                                                                             |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| exposureId            | ExposureId is an identifier in ESW/DMS for a single exposure. The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time when the ExposureId is created. |
| exposureTime          | Length in seconds of the current exposure                                                                                                                                                                                                                                                               |
| remainingExposureTime | Number of seconds remaining in current exposure • Should count down in seconds – no faster than 1 Hz                                                                                                                                                                                                    |

### Wavefront Detector Systems

There are many detector systems in the TMT Software System that are not part of the science  data chain. This section considers standardized events for wave front sensors and guider  detectors that will need to be visualized by ESW user interfaces. The events described are made  as simple as needed to enable this functionality. 

```text
Subsystem teams or TMT Systems Engineering will need to  
determine if the subsystem wavefront detector images or guide  
camera detector images need to be visualized or manipulated by  
ESW user interfaces. 
```

This Observe Event design is integrated with the ESW.VIZ subsystem (RD02), which allows  systems to publish images on named streams in order to minimize impact on WFS and Guider  Detector Systems. Within VIZ subsystems providing images can POST their images to a  Transfer Service on a named stream. Other notes subscribe to the stream and receive the  images when they are published.

The following are assumptions regarding WFS and Guider images in the TMT Software System.

- WFS and Guider images do not rely on header/metadata collection by DMS. WFS and Guider images will have headers, but the images from WFS and guiders are  transient and are not persisted by DMS. A detector system creating a WFS or Guider image  will create the header metadata itself or read values from the CSW Event Service.
- World Coordinate System for WFS and Guider images is determined independent of ESW or  OSW. This is assumed because ESW has no way of determining the WCS parameters for WFS or  Guider images. It is necessary that these systems create the WCS data themselves.
- WFS and Guider images are transient and are not written to or transferred to the DMS as with  science and calibration images. There are no requirements to automatically save images of this type unless specifically  requested.
- Some WFS/Guider images may occasionally be saved for diagnostic purposes. The DMS will allow blob data to be stored and associated with specific key-words.  But these images would be in the Engineering Database and will not be in the  science archive. These images would be captured and stored through the action  of a user.
- WFS and Guider images are not associated with observer observations or proposals. Science images are associated with science programs and observations. This is not needed  for WFS and Guider images and isn't supported.

A subsystem publishing an image to a VIZ stream indicates success or failure.

| Observe Event  | Description                                                                                  | Probable Publisher                                   |
| -------------- | -------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| publishSuccess | Indicates the WFS or guider detector system has  successfully published an image to VBDS     | VBDS Transfer Service or  possibly Detector Assembly |
| publishFailed  | Indicates that a WFS or guider detector system  has failed while publishing an image to VBDS | Detector Assembly or  possibly VBDS Transfer         |

#### WFS and Guider Detector Exposure State Event

An exposure state Observe Event called wfsDetectorExposureState is provided as a state  variable to indicate the current state of the wavefront sensor or guider detector system. The  Exposure State Event groups parameters that change relatively slowly, and this event should be  published whenever any of its parameters changes.

The Exposure State Event includes the following mandatory parameters, but a  detector system may add their own as well.

| Parameters         | Description                                                                                                                                                                                                                                                                                                                                                               |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| exposureId         | ExposureId is an identifier in ESW/DMS for a single exposure. The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with an included ObsId or when no ObsId is present, in the standalone format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time when the ExposureId is created.                                                                   |
| exposureInProgress | String identification of detector system (should be a single phrase with  no spaces e.g., my_detector). A Boolean indicating if detector system is acquiring an exposure  • Delimited by exposureStart and exposureEnd. exposureInProgress should be false if abortInProgress is true (TBD)                                                                               |
| abortInProgress    | Indicates that an abort has been requested and is underway                                                                                                                                                                                                                                                                                                                |
| isAborted          | Indicates that an abort has occurred and is completed. abortInProgress should be false when isAborted is true. isAborted should be set to false with the next exposure                                                                                                                                                                                                    |
| operationalState   | Enumeration indicating if the detector system is available and  operational. READY, BUSY, ERROR.  READY indicates system can execute exposures. BUSY indicates system is BUSY most likely acquiring data. ERROR indicates the detector system is in an error state. This could  happen as a result of a command or a spontaneous failure. Corrective  action is required. |
| errorMessage       | An optional parameter that can be included when the detector system  is in the ERROR operationalState. This value should be cleared and removed from the state when the  operationalState returns to READY                                                                                                                                                                |

This event is standardized to allow ESW user interfaces to have minimal, uniform information  about the state of any WFS or Guider Detector system.

#### WFS and Guider Detector Exposure Data Event

At this time there is no WFS and Guider Detector Exposure Event. Any specific telemetry for  these detector systems is available in their API model files and will not at this time be  standardized.

### Sequencers

Moving up from the acquisition of science detector data, it is necessary to have events that  indicate activities for each observation and the acquisition process. The overall execution of an  observation’s science Sequence is under the control of the master sequencer of ESW.OCS.  During the acquisition process there may be additional OCS Sequencers that control the overall  acquisition process. Regardless, the OCS Sequencer publishes Observe Events. 

There are quite a few Observe Events related to the acquisition process as defined in the  workflows (RD07) by the three phases:

- Preset – moving the telescope and subsystems to point to the sky coordinates of the  observation and configurating all the other subsystems needed for the observation.
- Guide Star Acquisition – locking the telescope to the sky, closing loops, and delivering the  telescope to the predefined hotspot needed for the science observation.
- Science Target Acquisition – any additional adjustments specific to a science observation  needed after the previous phase.

The following table shows the events related to the overall observation and its acquisition

| Observe Event     | Description                                                                                                                                                                                                                                                                                                                              | Probable Publisher |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------ |
| observationStart  | Indicates the start of execution of actions related  to an observation including acquisition and  science data acquisition                                                                                                                                                                                                               | OCS Sequencer      |
| presetStart       | Indicates the start of the preset phase of  acquisition                                                                                                                                                                                                                                                                                  | OCS Sequencer      |
| presetEnd         | Indicates the end of the preset phase of  acquisition                                                                                                                                                                                                                                                                                    | OCS Sequencer      |
| guidestarAcqStart | Indicates the start of locking the telescope to the  sky with guide and WFS targets                                                                                                                                                                                                                                                      | OCS Sequencer      |
| guidestarAcqEnd   | Indicates the end of locking the telescope to the  sky with guide and WFS targets                                                                                                                                                                                                                                                        | OCS Sequencer      |
| scitargetAcqStart | Indicates the start of acquisition phase where  science target is peaked up as needed after  guidestar locking                                                                                                                                                                                                                           | OCS Sequencer      |
| scitargetAcqEnd   | Indicates the end of acquisition phase where  science target is centered as needed after  guidestar locking                                                                                                                                                                                                                              | OCS Sequencer      |
| obseveStart       | Indicates the start of execution of actions related  to an Observe command                                                                                                                                                                                                                                                               | OCS Sequencer      |
|                   | EXPOSURE OBSERVE EVENTS DISCUSSED EARLIER OCCUR HERE                                                                                                                                                                                                                                                                                     |                    |
| observeEnd        | Indicates the end of execution of actions related  to an Observe command                                                                                                                                                                                                                                                                 | OCS Sequencer      |
| observationEnd    | Indicates the end of execution of actions related  to an observation including acquisition and  science data acquisition.                                                                                                                                                                                                                | OCS Sequencer      |
| observePaused     | Indicates that a user has paused the current  observation Sequence which will happen after  the current step concludes                                                                                                                                                                                                                   | OCS Sequencer      |
| observeResumed    | Indicates that a user has resumed a paused  observation Sequence                                                                                                                                                                                                                                                                         | OCS Sequencer      |
| downtimeStart     | Indicates that something has occurred that  interrupts the normal observing workflow and  time accounting. This event will have a hint (TBD) that indicates  the cause of the downtime for statistics.  Examples are: weather, equipment or other  technical failure, etc. Downtime is ended by the start of an observation  or exposure | OCS Sequencer      |

#### Downtime Considerations

The downtimeStart Observe Event is provided to allow the operator to indicate that there is a  problem that is hindering the normal observing workflow. The event includes a category that the  downtime will be associated with. Common examples are weather or equipment failure. The  categories and usage will be worked out at a later time. 
Sometimes metrics and statistics subtract the time associated with weather loss or technical  failures. It is challenging to assign downtime algorithmically. We anticipate that the telescope  observing assistant will assign a category through a user interface program provided by  ESW.HCMS. 
The downtime will be started by the downtimeStart event and ended by the next observationStart  or exposureStart event.

## How to create any observe event

The following snippet shows how to create a observe event

Scala
:   @@snip [EventsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #observe-event }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #observe-event }

## JSON Serialization

Events can be serialized to JSON. The library has provided **JsonSupport** helper class and methods to serialize Status, Observe and System events.

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #json-serialization }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #json-serialization }

## Unique Key Constraint

By choice, a ParameterSet in either **ObserveEvent** or **SystemEvent** event will be optimized to store only unique keys. 
When using `add` or `madd` methods on events to add new parameters, if the parameter being added has a key which is already present in the `paramSet`,
the already stored parameter will be replaced by the given parameter. 
 
@@@ note

If the `Set` is created by component developers and given directly while creating an event, then it will be the responsibility of component developers to maintain uniqueness with
parameters based on key.

@@@    

Here are some examples that illustrate this point:

Scala
:   @@snip [CommandsTest.scala](../../../../examples/src/test/scala/example/params/EventsTest.scala) { #unique-key }

Java
:   @@snip [JEventsTest.java](../../../../examples/src/test/java/example/params/JEventsTest.java) { #unique-key }
