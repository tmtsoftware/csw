package csw.messages.params.models

import csw.messages.TMTSerializable
import enumeratum.{Enum, EnumEntry}
import spray.json.JsonFormat

import scala.collection.immutable

sealed abstract class Units(name: String, description: String) extends EnumEntry with TMTSerializable {
  // Should parameterize Units so concat can be created concat[A, B]
  override def toString: String = "[" + name + "]"
}

object Units extends Enum[Units] {

  import csw.messages.params.formats.JsonSupport._

  override def values: immutable.IndexedSeq[Units] = findValues
  implicit val format: JsonFormat[Units]           = enumFormat(this)

  // SI units
  object angstrom    extends Units("Anstrom", "10 -1 nm")
  object arcmin      extends Units("arcmin", "arc minute; angular measurement")
  object arcsec      extends Units("arcsec", "arc second: angular measurement")
  object day         extends Units("d", "day - 24 hours")
  object degree      extends Units("deg", "degree: agular measurement 1/360 of full rotation")
  object elvolt      extends Units("eV", "electron volt 1.6022x10-19 J")
  object gram        extends Units("g", "gram 10-3 kg")
  object hour        extends Units("h", "hour 3.6x10+3 s")
  object hertz       extends Units("Hz", "frequency")
  object joule       extends Units("J", "Joule: energy N m")
  object kelvin      extends Units("K", "Kelvin: temperature with a null point at absolute zero")
  object kilogram    extends Units("kg", "kilogram, base unit of mass in SI")
  object kilometer   extends Units("km", "kilometers - 10+3 m")
  object liter       extends Units("l", "liter, metric unit of volume 10+3 cm+3")
  object meter       extends Units("m", "meter: base unit of length in SI")
  object marcsec     extends Units("mas", "milli arc second: angular measurement 10-3 arcsec")
  object millimeter  extends Units("mm", "millimeters - 10-3 m")
  object millisecond extends Units("ms", "milliseconds - 10-3 s")
  object micron      extends Units("µm", "micron: alias for micrometer")
  object micrometer  extends Units("µm", "micron: 10-6 m")
  object minute      extends Units("min", "minute 6x10+1 s")
  object newton      extends Units("N", "Newton: force")
  object pascal      extends Units("Pa", "Pascal: pressure")
  object radian
      extends Units("rad", "radian: angular measurement of the ratio between the length of an arc and its radius")
  object second      extends Units("s", "second: base unit of time in SI")
  object sday        extends Units("sday", "sidereal day is the time of one rotation of the Earth: 8.6164x10+4 s")
  object steradian   extends Units("sr", "steradian: unit of solid angle in SI - rad+2")
  object microarcsec extends Units("µas", "micro arcsec: angular measurement")
  object volt        extends Units("V", "Volt: electric potential or electromotive force")
  object watt        extends Units("W", "Watt: power")
  object week        extends Units("wk", "week - 7 d")
  object year        extends Units("yr", "year - 3.6525x10+2 d")

  // CGS units
  object coulomb    extends Units("C", "coulomb: electric charge")
  object centimeter extends Units("cm", "centimeter")
  object erg        extends Units("erg", "erg: CGS unit of energy")

  // Astrophsics units
  object au        extends Units("AU", "astronomical unit: approximately the mean Earth-Sun distance")
  object jansky    extends Units("Jy", "Jansky: spectral flux density - 10-26 W/Hz m+2")
  object lightyear extends Units("lyr", "light year - 9.4607x10+15 m")
  object mag       extends Units("mag", "stellar magnitude")

  // Imperial units
  object cal   extends Units("cal", "thermochemical calorie: pre-SI metric unit of energy")
  object foot  extends Units("ft", "international foot - 1.2x10+1 inch")
  object inch  extends Units("inch", "international inch - 2.54 cm")
  object pound extends Units("lb", "international avoirdpois pound - 1.6x10+1 oz")
  object mile  extends Units("mi", "internatonal mile - 5.28x10+3 ft")
  object ounce extends Units("oz", "international avoirdpois ounce")
  object yard  extends Units("yd", "international yard - 3 ft")

  // Others - engineering
  object NoUnits extends Units("none", "scalar - no units specified")
  object encoder extends Units("enc", "encoder counts")
  object count   extends Units("ct", "counts as for an encoder or detector")
  object pix     extends Units("pix", "pixel")
}
