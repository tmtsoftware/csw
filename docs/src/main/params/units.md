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
| angstrom    | Angstrom          | 10 -1 nm |
| arcmin      | arcmin            | arc minute; angular measurement |
| arcsec      | arcsec            | arc second: angular measurement |
| day         | d                 | day - 24 hours |
| degree      | deg               | degree: agular measurement 1/360 of full rotation |
| elvolt      | eV                | electron volt 1.6022x10-19 J |
| gram        | g                 | gram 10-3 kg |
| hour        | h                 | hour 3.6x10+3 s |
| hertz       | Hz                | frequency |
| joule       | J                 | Joule: energy N m |
| kelvin      | K                 | Kelvin: temperature with a null point at absolute zero |
| kilogram    | kg                | kilogram, base unit of mass in SI |
| kilometer   | km                | kilometers - 10+3 m |
| liter       | l                 | liter, metric unit of volume 10+3 cm+3 |
| meter       | m                 | meter: base unit of length in SI |
| marcsec     | mas               | milli arc second: angular measurement 10-3 arcsec |
| millimeter  | mm                | millimeters - 10-3 m |
| millisecond | ms                | milliseconds - 10-3 s |
| micron      | µm                | micron: alias for micrometer |
| micrometer  | µm                | micron: 10-6 m |
| minute      | min               | minute 6x10+1 s |
| newton      | N                 | Newton: force |
| pascal      | Pa                | Pascal: pressure |
| radian      | rad               | radian: angular measurement of the ratio between the length of an arc and its radius |
| second      | s                 | second: base unit of time in SI |
| sday        | sday              | sidereal day is the time of one rotation of the Earth: 8.6164x10+4 s |
| steradian   | sr                | steradian: unit of solid angle in SI - rad+2 |
| microarcsec | µas               | micro arcsec: angular measurement |
| volt        | V                 | Volt: electric potential or electromotive force |
| watt        | W                 | Watt: power |
| week        | wk                | week - 7 d |
| year        | yr                | year - 3.6525x10+2 d |

## CGS Units
| Name          | Abbreviation    | Description               |
| :-----------: |:--------------: | :-------------------------|
| coulomb    | C                  | coulomb: electric charge |
| centimeter | cm                 | centimeter |
| erg        | erg                | erg: CGS unit of energy |

## Astrophysical Units
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| au         | AU                 | astronomical unit: approximately the mean Earth-Sun distance |
| jansky     | Jy                 | Jansky: spectral flux density - 10-26 W/Hz m+2 |
| lightyear  | lyr                | light year - 9.4607x10+15 m |
| mag        | mag                | stellar magnitude |


## Imperial Units
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| cal        | cal                | thermochemical calorie: pre-SI metric unit of energy |
| foot       | ft                 | international foot - 1.2x10+1 inch |
| inch       | inch               | international inch - 2.54 cm |
| pound      | lb                 | international avoirdupois pound - 1.6x10+1 oz |
| mile       | mi                 | international mile - 5.28x10+3 ft |
| ounce      | oz                 | international avoirdupois ounce |
| yard       | yd                 | international yard - 3 ft |

## Datetime units
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| tai    | tai                | TAI Time unit |
| utc    | utc                | UTC Time unit |

## Others - Engineering
| Name          | Abbreviation    | Description                                                                |
| :-----------: |:--------------: | :--------------------------------------------------------------------------|
| NoUnits    | none               | scalar - no units specified |
| encoder    | enc                | encoder counts |
| count      | ct                 | counts as for an encoder or detector |
| pix        | pix                | pixel |

## Usage Examples

Scala
:   @@snip [KeysAndParametersTest.scala](../../../../examples/src/test/scala/example/params/KeysAndParametersTest.scala) { #units }

Java
:   @@snip [JKeysAndParametersTest.java](../../../../examples/src/test/java/example/params/JKeysAndParametersTest.java) { #units }

## Source Code for Examples

* [Scala Example]($github.base_url$/examples/src/test/scala/example/params/KeysAndParametersTest.scala)
* [Java Example]($github.base_url$/examples/src/test/java/example/params/JKeysAndParametersTest.java)