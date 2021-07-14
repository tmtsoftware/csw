package csw.params.core.models

import enumeratum.{Enum, EnumEntry}

sealed trait CalibrationLevel extends EnumEntry

object CalibrationLevel extends Enum[CalibrationLevel] {
  override def values: IndexedSeq[CalibrationLevel] = findValues

  case object Raw                         extends CalibrationLevel
  case object Uncalibrated                extends CalibrationLevel
  case object Calibrated                  extends CalibrationLevel
  case object ScienceProduct              extends CalibrationLevel
  case object AfterAnalysisScienceProduct extends CalibrationLevel
}

sealed abstract class TYP(description: String) extends EnumEntry {

  /**
   * Represents a string with entryName and description of a TYP
   */
  def longName: String = entryName + " - " + description

  /**
   * Represents the name of the TYP e.g SCI
   */
  def name: String = entryName
}

object TYP extends Enum[TYP] {
  override def values: IndexedSeq[TYP] = findValues

  case object SCI extends TYP("Science exposure")
  case object CAL extends TYP("Calibration exposure")
  case object ARC extends TYP("Wavelength calibration")
  case object IDP extends TYP("Instrumental dispersion")
  case object DRK extends TYP("Dark")
  case object MDK extends TYP("Master dark")
  case object FFD extends TYP("Flat field")
  case object NFF extends TYP("Normalized flat field")
  case object BIA extends TYP("Bias exposure")
  case object TEL extends TYP("Telluric standard")
  case object FLX extends TYP("Flux standard")
  case object SKY extends TYP("Sky background exposure")
}

case class TYPLevel(typ: TYP, calibrationLevel: CalibrationLevel) {
  override def toString: String = s"${typ.entryName}${CalibrationLevel.indexOf(calibrationLevel)}"

  def calibrationLevelNumber: Int = CalibrationLevel.indexOf(calibrationLevel)
}

object TYPLevel {
  private def parseCalibrationLevel(calibrationLevel: String): CalibrationLevel =
    try CalibrationLevel.values(calibrationLevel.toInt)
    catch {
      case ex: Exception =>
        throw new IllegalArgumentException(
          s"Failed to parse calibration level $calibrationLevel: ${ex.getMessage}. Calibration level should be a digit."
        )
    }

  def apply(typLevel: String): TYPLevel = {
    require(typLevel.length == 4, "TYPLevel must be a 3 character TYP followed by a calibration level")
    val (typ, calibrationLevel) = typLevel.splitAt(typLevel.length - 1)
    val level                   = parseCalibrationLevel(calibrationLevel)
    TYPLevel(TYP.withNameInsensitive(typ), level)
  }
}
