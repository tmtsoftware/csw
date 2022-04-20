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
  case object angstrom    extends Units("Angstrom", "angstrom")
  case object alpha       extends Units("alpha", "alpha: fine structure constant")
  case object ampere      extends Units("A", "ampere: unit of electric current")
  case object arcmin      extends Units("arcmin", "arc minute; angular measurement")
  case object arcsec      extends Units("arcsec", "arc second: angular measurement")
  case object bar         extends Units("bar", "bar: metric ton of pressure")
  case object candela     extends Units("candela ", "candela(lumen/sr)")
  case object day         extends Units("d", "day")
  case object degree      extends Units("deg", "degree: angular measurement 1/360 of full rotation")
  case object degC        extends Units("degC", "Degree Celsius K")
  case object degF        extends Units("degF", "Fahrenheit")
  case object elvolt      extends Units("eV", "electron volt")
  case object gauss       extends Units("gauss", "gauss")
  case object gram        extends Units("g", "gram")
  case object hertz       extends Units("Hz", "frequency")
  case object henry       extends Units("henry", "Henry")
  case object hour        extends Units("h", "hour")
  case object joule       extends Units("J", "Joule: energy")
  case object kelvin      extends Units("K", "Kelvin: temperature with a null point at absolute zero")
  case object kilogram    extends Units("kg", "kilogram, base unit of mass in SI")
  case object kilometer   extends Units("km", "kilometers")
  case object liter       extends Units("l", "liter, metric unit of volume")
  case object lm          extends Units("lm", "lumen")
  case object lsun        extends Units("lsun", "solar luminosity")
  case object lx          extends Units("lx", "lux(lm/m2)")
  case object mas         extends Units("mas", "milli arc second")
  case object me          extends Units("me", "me(electron_mass)")
  case object meter       extends Units("m", "meter: base unit of length in SI")
  case object microarcsec extends Units("µas", "micro arcsec: angular measurement")
  case object millimeter  extends Units("mm", "millimeters")
  case object millisecond extends Units("ms", "milliseconds")
  case object micron      extends Units("µm", "micron: alias for micrometer")
  case object micrometer  extends Units("µm", "micron")
  case object minute      extends Units("min", "minute")
  case object MJD         extends Units("MJD", "Mod. Julian Date")
  case object mol         extends Units("mol", "mole- unit of substance")
  case object month       extends Units("month", "Month name or number")
  case object mmyy        extends Units("mmyy", "mmyy: Month/Year")
  case object mu0         extends Units("mu0", "mu0: magnetic constant")
  case object muB         extends Units("muB", "Bohr magneton")
  case object nanometer   extends Units("nm", "nanometers")
  case object newton      extends Units("N", "Newton: force")
  case object ohm         extends Units("ohm", "Ohm")
  case object pascal      extends Units("Pa", "Pascal: pressure")
  case object pi          extends Units("pi", "pi")
  case object pc          extends Units("pc", "parsec")
  case object ppm         extends Units("ppm", "part per million")
  case object radian extends Units("rad", "radian: angular measurement of the ratio between the length of an arc and its radius")
  case object R      extends Units("R", "gas_constant")
  case object second extends Units("s", "second: base unit of time in SI")
  case object sday   extends Units("sday", "sidereal day is the time of one rotation of the Earth")
  case object steradian extends Units("sr", "steradian: unit of solid angle in SI")
  case object volt      extends Units("V", "Volt: electric potential or electromotive force")
  case object watt      extends Units("W", "Watt: power")
  case object Wb        extends Units("Wb", "Weber")
  case object week      extends Units("wk", "week")
  case object year      extends Units("yr", "year")

  // CGS units
  case object coulomb    extends Units("C", "coulomb: electric charge")
  case object centimeter extends Units("cm", "centimeter")
  case object D          extends Units("Debye", "Debye(dipole) A electric dipole moment ")
  case object dyn        extends Units("dyne", "dyne: Unit of force ")
  case object erg        extends Units("erg", "erg: CGS unit of energy")

  // Astropyhsics units
  case object au extends Units("AU", "astronomical unit: approximately the mean Earth-Sun distance")
  case object a0
      extends Units(
        "a0",
        "bohr radius: probable distance between the nucleus and the electron in a hydrogen atom in its ground state"
      )
  case object c       extends Units("c", "c: speed of light")
  case object cKayser extends Units("cKayser", "cKayser")
  case object crab
      extends Units("crab", "Crab: astrophotometrical unit for measurement of the intensity of Astrophysical X-ray sources")
  case object damas     extends Units("d:m:s", "damas: degree arcminute arcsecond (sexagesimal angle from degree)")
  case object e         extends Units("e", "electron charge")
  case object Earth     extends Units("earth", "earth (geo) unit")
  case object eps0      extends Units("eps0", "electric constant")
  case object F         extends Units("F", "Farad: F")
  case object G         extends Units(name = "G", "gravitation constant")
  case object geoMass   extends Units("geoMass", "Earth Mass")
  case object hm        extends Units("hm", "hour minutes (sexagesimal time from hours)")
  case object hms       extends Units("hms", "hour minutes seconds (sexagesimal time from hours)")
  case object hhmmss    extends Units("HH:MM:SS", "hour minutes seconds (sexagesimal time)")
  case object jansky    extends Units("Jy", "Jansky: spectral flux density ")
  case object jd        extends Units("jd", "Julian Day")
  case object jovmass   extends Units("jovMass", "Jupiter mass")
  case object lightyear extends Units("lyr", "light year")
  case object mag       extends Units("mag", "stellar magnitude")
  case object mjup      extends Units("Mjup", "Jupiter mass")
  case object mp        extends Units("mp", "proton_mass")
  case object minsec    extends Units("m:s", "minutes seconds (sexagesimal time from minutes)")
  case object msun      extends Units("Msun", "solar mass")
  case object photon    extends Units("photon", "photon")
  case object rgeo      extends Units("Rgeo", "Earth radius (eq)")
  case object rjup      extends Units("Rjup", "Jupiter Radius(eq)")
  case object rsun      extends Units("Rsun", "solar radius")
  case object rydberg   extends Units("Rydberg", "energy of the photon whose wavenumber is the Rydberg constant")
  case object seimens   extends Units("seimens", "Seimens")
  case object tesla     extends Units("tesla", "Tesla")
  case object u         extends Units("u", "atomic mass unit")

  // Imperial units
  case object barn  extends Units("barn", "barn: metric unit of area")
  case object cal   extends Units("cal", "thermochemical calorie: pre-SI metric unit of energy")
  case object foot  extends Units("ft", "international foot - 1.2E1 inch")
  case object inch  extends Units("inch", "international inch - 2.54 cm")
  case object pound extends Units("lb", "international avoirdupois pound - 1.6E1 oz")
  case object mile  extends Units("mi", "international mile")
  case object ounce extends Units("oz", "international avoirdupois ounce")
  case object yard  extends Units("yd", "international yard - 3 ft")

  // Others - engineering
  case object NoUnits extends Units("none", "scalar - no units specified")
  case object bit     extends Units("bit", "bit: binary value of 0 or 1")
  case object encoder extends Units("enc", "encoder counts")
  case object count   extends Units("ct", "counts as for an encoder or detector")
  case object mmhg    extends Units("mmHg", "millimetre of mercury is a manometric unit of pressure")
  case object percent extends Units("percent", "percentage")
  case object pix     extends Units("pix", "pixel")

  // Datetime units
  case object tai      extends Units("TAI", "TAI time unit")
  case object utc      extends Units("UTC", "UTC time unit")
  case object date     extends Units("date", "date")
  case object datetime extends Units("datetime", "date/time")
}
