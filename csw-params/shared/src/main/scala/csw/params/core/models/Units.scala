/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.core.models
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
 * A class representing units for TMT
 *
 * @param name of the unit
 * @param description of the unit
 */
sealed abstract class Units(name: String, description: String) extends EnumEntry {
  // Should parameterize Units so concat can be created concat[A, B]
  override def toString: String = name

  override lazy val entryName: String = getClass.getSimpleName.stripSuffix("$")

  /**
   * The name of the unit
   */
  def getName: String = name

  /**
   * The description of unit
   */
  def getDescription: String = description
}

object Units extends Enum[Units] {

  /**
   * A Seq of all values that are Units
   */
  override def values: immutable.IndexedSeq[Units] = findValues

  // SI units
  case object angstrom    extends Units("Angstrom", "10 -1 nm")
  case object alpha       extends Units("alpha", "alpha: fine structure constant 7.29735×10–3")
  case object ampere      extends Units("A", "ampere: unit of electric current")
  case object arcmin      extends Units("arcmin", "arc minute; angular measurement")
  case object arcsec      extends Units("arcsec", "arc second: angular measurement")
  case object bar         extends Units("bar", "bar: 10+5Pa (kg⋅m–1⋅s–2) metric ton of pressure")
  case object candela     extends Units("candela ", "candela(lumen/sr) ")
  case object day         extends Units("d", "day - 24 hours")
  case object degree      extends Units("deg", "degree: agular measurement 1/360 of full rotation")
  case object degC        extends Units("degC", "Degree Celsius K")
  case object degF        extends Units("degF", "Fahrenheit 5.55556×10–1K")
  case object elvolt      extends Units("eV", "electron volt 1.6022x10-19 J")
  case object gauss       extends Units("gauss", "gauss: 10–4T (kg⋅s–2⋅A–1)")
  case object gram        extends Units("g", "gram 10-3 kg")
  case object hbar        extends Units("hbar", "hbar(Planck constant): 1.05457×10–34kg⋅m+2⋅s–1")
  case object hertz       extends Units("Hz", "frequency")
  case object henry       extends Units("henry", "H (kg⋅m+2⋅s–2⋅A–2)")
  case object hour        extends Units("h", "hour 3.6x10+3 s")
  case object joule       extends Units("J", "Joule: energy N m")
  case object k           extends Units("k", "k(Boltzmann constant) 1.380649×10−23 J⋅K−1")
  case object kelvin      extends Units("K", "Kelvin: temperature with a null point at absolute zero")
  case object kilogram    extends Units("kg", "kilogram, base unit of mass in SI")
  case object kilometer   extends Units("km", "kilometers - 10+3 m")
  case object liter       extends Units("l", "liter, metric unit of volume 10+3 cm+3")
  case object lm          extends Units("lm", "lumen: 7.95775×10–2cd")
  case object lsun        extends Units("lsun", "solar luminosity: 3.826×10+26W (kg⋅m+2⋅s–3) ")
  case object lx          extends Units("lx", "lux(lm/m2): 7.95775×10–2m–2⋅cd")
  case object mas         extends Units("mas", "milli arc second: angular measurement 10-3 arcsec")
  case object me          extends Units("me", "me(electron_mass): 9.10938×10–31kg")
  case object meter       extends Units("m", "meter: base unit of length in SI")
  case object microarcsec extends Units("µas", "micro arcsec: angular measurement")
  case object millimeter  extends Units("mm", "millimeters - 10-3m")
  case object millisecond extends Units("ms", "milliseconds - 10-3s")
  case object micron      extends Units("µm", "micron: alias for micrometer")
  case object micrometer  extends Units("µm", "micron: 10-6 m")
  case object minute      extends Units("min", "minute 6x10+1 s")
  case object MJD         extends Units("MJD", "Mod. Julian Date (JD–2400000.5) 8.64×10+4s")
  case object mol         extends Units("mol", "mole- unit of substance")
  case object month       extends Units("month", "Month name or number- 2.6298×10+6s")
  case object mmyy        extends Units("mmyy", "Month/Year - 2.6298×10+6s")
  case object mu0         extends Units("mu0", "magnetic constant: 1.25664×10–6kg⋅m⋅s–2⋅A–2")
  case object muB         extends Units("muB", "Bohr magneton: 9.27401×10–28m+2⋅A")
  case object nanometer   extends Units("nm", "nanometers 10-9 m")
  case object newton      extends Units("N", "Newton: force")
  case object ohm         extends Units("ohm", "Ohm (kg⋅m+2⋅s–3⋅A–2)")
  case object pascal      extends Units("Pa", "Pascal: pressure")
  case object pi          extends Units("pi", "pi: 3.14159")
  case object pc          extends Units("pc", "parsec: 3.0857×10+16m")
  case object ppm         extends Units("ppm", "part per million: 10–6")
  case object radian extends Units("rad", "radian: angular measurement of the ratio between the length of an arc and its radius")
  case object R      extends Units("R", "gas_constant: 8.3143kg⋅m+2⋅s–2⋅K–1⋅mol–1")
  case object second extends Units("s", "second: base unit of time in SI")
  case object sday   extends Units("sday", "sidereal day is the time of one rotation of the Earth: 8.6164x10+4 s")
  case object steradian extends Units("sr", "steradian: unit of solid angle in SI - rad+2")
  case object volt      extends Units("V", "Volt: electric potential or electromotive force")
  case object watt      extends Units("W", "Watt: power")
  case object Wb        extends Units("Wb", "Weber(V⋅s) (kg⋅m+2⋅s–2⋅A–1)")
  case object week      extends Units("wk", "week - 7 d")
  case object year      extends Units("yr", "year - 3.6525x10+2 d")

  // CGS units
  case object coulomb    extends Units("C", "coulomb: electric charge")
  case object centimeter extends Units("cm", "centimeter")
  case object D          extends Units("Debye", "Debye(dipole) 3.33333×10–30m⋅s⋅A electric dipole moment ")
  case object dyn        extends Units("dyne", "dyne: Unit of force 10–5N (kg⋅m⋅s–2)")
  case object erg        extends Units("erg", "erg: CGS unit of energy")

  // Astropyhsics units
  case object au extends Units("AU", "astronomical unit: approximately the mean Earth-Sun distance")
  case object a0
      extends Units(
        "a0",
        "bohr radius: (5.29177×10–11m) probable distance between the nucleus and the electron in a hydrogen atom in its ground state"
      )
  case object c       extends Units("c", "speed of light 2.99792×10+8m⋅s–1")
  case object cKayser extends Units("cKayser", "m–1")
  case object crab
      extends Units("crab", "Crab: astrophotometrical unit for measurement of the intensity of Astrophysical X-ray sources")
  case object damas     extends Units("d:m:s", "damas: degree arcminute arcsecond (sexagesimal angle from degree) 2.77778×10–3")
  case object e         extends Units("e", "electron charge 1.60218×10–19C (s⋅A)")
  case object Earth     extends Units("earth", "earth (geo) unit")
  case object eps0      extends Units("eps0", "electric constant")
  case object F         extends Units("F", "Farad: F (kg–1⋅m–2⋅s+4⋅A+2)")
  case object G         extends Units(name = "G", "gravitation constant: 6.673×10–11 kg–1⋅m+3⋅s–2")
  case object geoMass   extends Units("geoMass", "Earth Mass: 5.9742×10+24kg")
  case object hm        extends Units("hm", "hour minutes (sexagesimal time from hours) - 3.6×10+3s")
  case object hms       extends Units("hms", "hour minutes seconds (sexagesimal time from hours) - 3.6×10+3s")
  case object hhmmss    extends Units("HH:MM:SS", "hour minutes seconds (sexagesimal time)- 3.6×10+3s")
  case object jansky    extends Units("Jy", "Jansky: spectral flux density - 10-26 W/Hz m+2")
  case object jd        extends Units("jd", "Julian Day: 8.64×10+4s")
  case object jovmass   extends Units("jovMass", "Jupiter mass: 1.8986×10+27kg ")
  case object lightyear extends Units("lyr", "light year - 9.4607x10+15 m")
  case object mag       extends Units("mag", "stellar magnitude")
  case object mjup      extends Units("Mjup", "Jupiter mass: 1.8986×10+27kg")
  case object mp        extends Units("mp", "proton_mass: 1.67266×10–27kg")
  case object minsec    extends Units("m:s", "minutes seconds (sexagesimal time from minutes)- 6×10+1s")
  case object msun      extends Units("Msun", "solar mass: 1.989×10+30kg")
  case object photon    extends Units("photon", "photon")
  case object rgeo      extends Units("Rgeo", "Earth radius (eq): 6.3781×10+6m")
  case object rjup      extends Units("Rjup", "Jupiter Radius(eq): 7.1492×10+7m")
  case object rsun      extends Units("Rsun", "solar radius: 6.9599×10+8m")
  case object rydberg   extends Units("Rydberg", "energy of the photon whose wavenumber is the Rydberg constant")
  case object seimens   extends Units("Seimens", "S (kg–1⋅m–2⋅s+3⋅A+2)")
  case object tesla     extends Units("tesla", "Tesla (kg⋅s–2⋅A–1)")
  case object u         extends Units("u", "atomic mass unit: 1.66054×10–27kg")

  // Imperial units
  case object barn  extends Units("barn", "barn: metric unit of area equal to 10−28 m+2")
  case object cal   extends Units("cal", "thermochemical calorie: pre-SI metric unit of energy")
  case object foot  extends Units("ft", "international foot - 1.2x10+1 inch")
  case object inch  extends Units("inch", "international inch - 2.54 cm")
  case object pound extends Units("lb", "international avoirdupois pound - 1.6x10+1 oz")
  case object mile  extends Units("mi", "international mile - 5.28x10+3 ft")
  case object ounce extends Units("oz", "international avoirdupois ounce")
  case object yard  extends Units("yd", "international yard - 3 ft")

  // Others - engineering
  case object NoUnits extends Units("none", "scalar - no units specified")
  case object bit     extends Units("bit", "bit: binary value of 0 or 1")
  case object byte    extends Units("byte", "byte: 8 bits")
  case object encoder extends Units("enc", "encoder counts")
  case object count   extends Units("ct", "counts as for an encoder or detector")
  case object mmhg    extends Units("mmHg", "mercury_mm : 1.33322×10+2Pa (kg⋅m–1⋅s–2)")
  case object percent extends Units("percent", "percentage 10-2")
  case object pix     extends Units("pix", "pixel")

  // Datetime units
  case object tai      extends Units("TAI", "TAI time unit")
  case object utc      extends Units("UTC", "UTC time unit")
  case object date     extends Units("date", "date: Fully qualified date - 8.64×10+4s")
  case object datetime extends Units("datetime", " Fully qualified date/time - 8.64×10+4s ")
}
