## Subsystem

TMT Observatory system is composed of many subsystems. The subsystems that are known participants in the TMT Software System are predefined and the list is covered under `Subsystem` enumeration. They are identified using a 3 letter abbreviation. 

**Susbsystem** values are used to construct **[Prefix](commands.html#Prefix)** and are used in communication vehicles such as Commands, Events and States.

@@@ note

Subsystems are made available via separate files, for consumption in Scala and Java code.

 * Import `csw.messages.params.models.Subsystem` for **Scala** 
 * Import `csw.messages.javadsl.JSubsystem` for **Java**.  

@@@

### List of Subsystems
 
| Abbreviation    | Susbsystem name             |
| :-------------: |:----------------------------| 
|  AOESW          | AO Executive Software |
|  APS            | Alignment and Phasing System |
|  CIS            | Communications and Information Systems |
|  CSW            | Common Software |
|  DMS            | Data Management System |
|  DPS            | Data Processing System |
|  ENC            | Enclosure |
|  ESEN           | Engineering Sensor System |
|  ESW            | Executive Software System |
|  GMS            | Global Metrology System Controls |
|  IRIS           | InfraRed Imaging Spectrometer |
|  IRMS           | Infrared Multi-Slit Spectrometer |
|  LGSF           | Lasert Guide Star Facility |
|  M1CS           | M1 Control System |
|  M2CS           | M2 Control System |
|  M3CS           | M3 Control System |
|  MCS            | Mount Control System |
|  NFIRAOS        | Narrow Field Infrared AO System |
|  NSCU           | NFIRAOS Science Calibration Unit |
|  OSS            | Observatory Safety System |
|  PFCS           | Prime Focus Camera Controls |
|  PSFR           | NFIRAOS AO PSF Reconstructor |
|  RTC            | NFIRAOS Real-time Controller |
|  RPG            | NFIRAOS AO Reconstructor Parameter Generator |
|  SCMS           | Site Conditions Monitoring System |
|  SOSS           | Science Operations Support System |
|  STR            | Telescope Structure |
|  SUM            | Summit Facility |
|  TCS            | Telescope Control System |
|  TINC           | Prime Focus Camera Controls |
|  WFOS           | Wide Field Optical Spectrometer |
|  TEST           | Testing System |
|  BAD            | Unknown/default Subsystem |

### Usage examples
The usage examples can be found in [Events](events.html), [Commands](commands.html), [States](states.html)