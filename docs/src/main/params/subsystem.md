# Subsystem

TMT Observatory system is composed of many subsystems. The subsystems that are known participants in the TMT Software
System are predefined and the list is covered under the `Subsystem` enumeration. They are identified using an
abbreviation typically of 3 or 4 letters.

**Susbsystem** values are used to construct **[Prefix](commands.html#Prefix)** and are used in communication vehicles
such as Commands, Events and States.

@@@ note

Subsystems are made available via separate files, for consumption in Scala and Java code.

* Import `csw.prefix.models.Subsystem` for **Scala**
* Import `csw.prefix.javadsl.JSubsystem` for **Java**.

@@@

## List of Subsystems

| Abbreviation | Susbsystem name                                                  |
| :----------: | :--------------------------------------------------------------- |
|    AOESW     | Adaptive Optics Executive Software                               |
|     APS      | Alignment and Phasing System                                     |
|     CIS      | Communications and Information Systems                           |
|     CLN      | Optical Cleaning Systems                                         |
|     CRYO     | Instrumentation Cryogenic Cooling System                         |
|     CSW      | Common Software                                                  |
|     DMS      | Data Management System                                           |
|     DPS      | Data Processing System                                           |
|     ENC      | Enclosure                                                        |
|     ESEN     | Engineering Sensors                                              |
|     ESW      | Executive Software                                               |
|     HNDL     | Optics Handling Equipment                                        |
|      HQ      | Observatory Headquarters                                         |
|     IRIS     | InfraRed Imaging Spectrometer                                    |
|     LGSF     | Laser Guide Star Facility                                        |
|    M1COAT    | M1COAT M1 Optical Coating System                                 |
|     M1CS     | M1CS M1 Control System                                           |
|     M1S      | M1S M1 Optics System                                             |
|    M2COAT    | M2/M3 Optical Coating System                                     |
|     M2S      | M2S M2 System                                                    |
|     M3S      | M3S M3 System                                                    |
|    MODHIS    | Multi-Object Diffraction-limited High-resolution IR Spectrograph |
|   NFIRAOS    | Narrow Field Infrared AO System                                  |
|     OSS      | Observatory Safety System                                        |
|     REFR     | Instrumentation Refrigerant Cooling System                       |
|     SCMS     | Site Conditions Monitoring System                                |
|     SER      | Services                                                         |
|     SOSS     | Science Operations Support Systems                               |
|     STR      | Structure                                                        |
|     SUM      | Summit Facilities                                                |
|     TCS      | Telescope Control System                                         |
|     TINS     | Test Instruments                                                 |
|     WFOS     | Wide Field Optical Spectrograph                                  |
|  Container   | Container subsystem                                              |

## Usage Examples

The usage examples can be found in [Events](events.html), [Commands](commands.html), [States](states.html)