# Units

Listed below are Units of Measurement, supported by TMT observatory framework and are available as Enumerated values. 
`Units` are optionally attached to `Parameter` Keys.

@@@ note

Units are made available via separate files, for consumption in Scala and Java code.

 * Import `csw.messages.params.models.Units` for **Scala** 
 * Import `csw.params.javadsl.JUnits` for **Java**.  

@@@

@@@ note

The set of supported Units will be modified as more required Units are discovered.

@@@ 

## Default Units for Keys

The default unit for `UTCTimeKey` and `TAITimeKey` (in Scala and Java both) is `utc` & `tai` respectively. For all the remaining keys, default unit is NoUnits.  

## SI Units

| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| angstrom    | Angstrom          | angstrom|
| alpha       | alpha             | alpha    |
| ampere       | A                | ampere    |
| arcmin      | arcmin            | arc minute; angular measurement |
| arcsec      | arcsec            | arc second: angular measurement |
| bar         | bar               | bar    |
| candela     | candela           | candela    |
| day         | d                 | day - 24 hours |
| degree      | deg               | degree: agular measurement 1/360 of full rotation |
| degC        | degC              | degree celsius    |
| degF        | degF               | Fahrenheit    |
| elvolt      | eV                | electron volt  |
| gauss       | gauss             | gauss  |
| gram        | g                 | gram |
| hertz       | Hz                | frequency |
| henry       | henry             | henry  |
| hour        | h                 | hour  |
| joule       | J                 | Joule: energy N m |
| kelvin      | K                 | Kelvin: temperature with a null point at absolute zero |
| kilogram    | kg                | kilogram, base unit of mass in SI |
| kilometer   | km                | kilometers |
| liter       | l                 | liter, metric unit of volume |
| lm          | lm                | lumen  |
| lsun        | lsun              | solar luminosity  |
| lx          | lx                | lux  |
| meter       | m                 | meter: base unit of length in SI |
| mas         | mas               | milli arc second: angular measurement |
| me          | me                | electron mass  |
| meter       | m                 | meter  |
| microarcsec | µas               | micro arcsec |
| millimeter  | mm                | millimeters|
| millisecond | ms                | milliseconds |
| micron      | µm                | micron: alias for micrometer |
| micrometer  | µm                | micron |
| minute      | min               | minute |
| MJD         | MJD               | Mod. Julian Date |
| mol         | mol               | mole |
| month       | month             | month |
| mmyy        | mmyy              | Month/Year |
| mu0         | mu0               | magnetic constant |
| muB         | muB               | bohr magneton |
| nanometer   | nm                | nanometers |
| newton      | N                 | Newton: force |
| ohm         | ohm               | ohm |
| pascal      | Pa                | Pascal: pressure |
| pi          | pi                | pi |
| pc          | pc                | parsec |
| ppm         | ppm               | part per million |
| radian      | rad               | radian: angular measurement of the ratio between the length of an arc and its radius |
| second      | s                 | second: base unit of time in SI |
| sday        | sday              | sidereal day is the time of one rotation of the Earth |
| steradian   | sr                | steradian: unit of solid angle in SI  |
| volt        | V                 | Volt: electric potential or electromotive force |
| watt        | W                 | Watt: power |
| Wb          | Wb                | weber |
| week        | wk                | week  |
| year        | yr                | year  |

## CGS Units
| Name          | Abbreviation    | Description               |
| :-----------: |:--------------: | :-------------------------|
| coulomb    | C                  | coulomb: electric charge |
| centimeter | cm                 | centimeter |
| D          | D                  | Debye(dipole) |
| dyn        | dyn                | dyne |
| erg        | erg                | erg: CGS unit of energy |

## Astrophysical Units
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| au         | AU                 | astronomical unit: approximately the mean Earth-Sun distance |
| a0         | a0                 | bohr radius |
| c          | c                  | speed of light |
| cKayser    | cKayser            | cKayser |
| crab       | crab               | crab |
| damas      | damas              | degree minute arcsecond |
| e          | e                  | electron charge |
| earth      | earth              | earth unit |
| F          | F                  | Farad |
| G          | G                  | Gravitation Constant |
| geoMass    | geoMass            | Earth Mass |
| hm         | hm                 | hour minute |
| hms        | hms                | hour minute seconds |
| hhmmss     | HH:MM:SS           | hour minutes seconds(sexagesimal time) |
| jansky     | Jy                 | Jansky: spectral flux density |
| jd         | jd                 | Julian Day |
| jovMass    | jovMass            | Jupiter Mass |
| lightyear  | lyr                | light year  |
| mag        | mag                | stellar magnitude |
| mjup       | mjup               | Jupiter mass |
| mp         | mp                 | proton mass |
| minsec     | m:s                | minute second  |
| msun       | msun               | solar mass |
| photon     | photon             | photon |
| rgeo       | Rgeo               | Earth radius |
| rjup       | Rjup               | Jupiter radius |
| rsun       | Rsun               | solar radius |
| rydberg    | Rydberg            | Rydberg |
| seimens    | seimens            | seimens |
| tesla      | tesla              | tesla |
| u          | u                  | atomic mass unit |



## Imperial Units
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| barn       | barn              | barn |
| cal        | cal                | thermochemical calorie: pre-SI metric unit of energy |
| foot       | ft                 | international foot |
| inch       | inch               | international inch |
| pound      | lb                 | international avoirdupois pound |
| mile       | mi                 | international mile |
| ounce      | oz                 | international avoirdupois ounce |
| yard       | yd                 | international yard |

## Datetime units
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| tai      | tai                | TAI Time unit |
| utc      | utc                | UTC Time unit |
| date     | date               | date |
| datetime | datetime           | datetime |

## Others - Engineering
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| NoUnits    | none               | scalar - no units specified |
| bit        | bit                | bit |
| encoder    | enc                | encoder counts |
| count      | ct                 | counts as for an encoder or detector |
| mmhg       | mmHg               | millimetre of  mercury |
| percent    | percent            | percentage |
| pix        | pix                | pixel |

## Usage Examples

Scala
:   @@snip [KeysAndParametersExample.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersExample.scala) { #units }

Java
:   @@snip [JKeysAndParametersExample.java](../../../../examples/src/test/java/example/params/JKeysAndParametersExample.java) { #units }

## Source Code for Examples

* [Scala Example]($github.base_url$/examples/src/test/scala/example/params/KeysAndParametersExample.scala)
* [Java Example]($github.base_url$/examples/src/test/java/example/params/JKeysAndParametersExample.java)