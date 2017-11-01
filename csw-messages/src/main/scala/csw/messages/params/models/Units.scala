package csw.messages.params.models

import com.trueaccord.scalapb.TypeMapper
import csw.messages.TMTSerializable
import csw_messages_params.units.PbUnits
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

sealed abstract class Units(name: String, description: String) extends EnumEntry with TMTSerializable {
  // Should parameterize Units so concat can be created concat[A, B]
//  override def toString: String = "[" + name + "]"
  def getName: String        = s"[$name]"
  def getDescription: String = description
}

object Units extends Enum[Units] with PlayJsonEnum[Units] {

  override def values: immutable.IndexedSeq[Units] = findValues
  implicit val typeMapper: TypeMapper[PbUnits, Units] =
    TypeMapper[PbUnits, Units](x ⇒ Units.withName(x.toString()))(x ⇒ PbUnits.fromName(x.toString).get)

  // SI units
  case object angstrom    extends Units("Anstrom", "10 -1 nm")
  case object arcmin      extends Units("arcmin", "arc minute; angular measurement")
  case object arcsec      extends Units("arcsec", "arc second: angular measurement")
  case object day         extends Units("d", "day - 24 hours")
  case object degree      extends Units("deg", "degree: agular measurement 1/360 of full rotation")
  case object elvolt      extends Units("eV", "electron volt 1.6022x10-19 J")
  case object gram        extends Units("g", "gram 10-3 kg")
  case object hour        extends Units("h", "hour 3.6x10+3 s")
  case object hertz       extends Units("Hz", "frequency")
  case object joule       extends Units("J", "Joule: energy N m")
  case object kelvin      extends Units("K", "Kelvin: temperature with a null point at absolute zero")
  case object kilogram    extends Units("kg", "kilogram, base unit of mass in SI")
  case object kilometer   extends Units("km", "kilometers - 10+3 m")
  case object liter       extends Units("l", "liter, metric unit of volume 10+3 cm+3")
  case object meter       extends Units("m", "meter: base unit of length in SI")
  case object marcsec     extends Units("mas", "milli arc second: angular measurement 10-3 arcsec")
  case object millimeter  extends Units("mm", "millimeters - 10-3 m")
  case object millisecond extends Units("ms", "milliseconds - 10-3 s")
  case object micron      extends Units("µm", "micron: alias for micrometer")
  case object micrometer  extends Units("µm", "micron: 10-6 m")
  case object minute      extends Units("min", "minute 6x10+1 s")
  case object newton      extends Units("N", "Newton: force")
  case object pascal      extends Units("Pa", "Pascal: pressure")
  case object radian
      extends Units("rad", "radian: angular measurement of the ratio between the length of an arc and its radius")
  case object second      extends Units("s", "second: base unit of time in SI")
  case object sday        extends Units("sday", "sidereal day is the time of one rotation of the Earth: 8.6164x10+4 s")
  case object steradian   extends Units("sr", "steradian: unit of solid angle in SI - rad+2")
  case object microarcsec extends Units("µas", "micro arcsec: angular measurement")
  case object volt        extends Units("V", "Volt: electric potential or electromotive force")
  case object watt        extends Units("W", "Watt: power")
  case object week        extends Units("wk", "week - 7 d")
  case object year        extends Units("yr", "year - 3.6525x10+2 d")

  // CGS units
  case object coulomb    extends Units("C", "coulomb: electric charge")
  case object centimeter extends Units("cm", "centimeter")
  case object erg        extends Units("erg", "erg: CGS unit of energy")

  // Astrophsics units
  case object au        extends Units("AU", "astronomical unit: approximately the mean Earth-Sun distance")
  case object jansky    extends Units("Jy", "Jansky: spectral flux density - 10-26 W/Hz m+2")
  case object lightyear extends Units("lyr", "light year - 9.4607x10+15 m")
  case object mag       extends Units("mag", "stellar magnitude")

  // Imperial units
  case object cal   extends Units("cal", "thermochemical calorie: pre-SI metric unit of energy")
  case object foot  extends Units("ft", "international foot - 1.2x10+1 inch")
  case object inch  extends Units("inch", "international inch - 2.54 cm")
  case object pound extends Units("lb", "international avoirdpois pound - 1.6x10+1 oz")
  case object mile  extends Units("mi", "internatonal mile - 5.28x10+3 ft")
  case object ounce extends Units("oz", "international avoirdpois ounce")
  case object yard  extends Units("yd", "international yard - 3 ft")

  // Others - engineering
  case object NoUnits extends Units("none", "scalar - no units specified")
  case object encoder extends Units("enc", "encoder counts")
  case object count   extends Units("ct", "counts as for an encoder or detector")
  case object pix     extends Units("pix", "pixel")
}
