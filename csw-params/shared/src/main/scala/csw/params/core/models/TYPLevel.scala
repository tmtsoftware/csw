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

  case object SCI extends TYP("Science image")
  case object CAL extends TYP("A calibration exposure")
  case object ARC extends TYP("Calibration for wavelength determination")
  case object IDP extends TYP("Instrumental Dispersion (discuss)")
  case object DRK extends TYP("Dark")
  case object MDK extends TYP("Master Dark")
  case object FFD extends TYP("Flat Field")
  case object NFF extends TYP("Normalized Flat Field (discuss)")
  case object BIA extends TYP("Bias exposure")
  case object TEL extends TYP("Telluric Standard")
  case object FLX extends TYP("Flux Standard")
  case object SKY extends TYP("Sky background exposure")
}

case class TYPLevel(typ: TYP, calibrationLevel: CalibrationLevel) {
  override def toString: String = s"${typ.entryName}${CalibrationLevel.indexOf(calibrationLevel)}"
}

object TYPLevel {
  private def parseCalibrationLevel(calibrationLevel: String): CalibrationLevel =
    try CalibrationLevel.values(calibrationLevel.toInt)
    catch {
      case ex: Exception =>
        throw new IllegalArgumentException(s"Failed to parse calibration level $calibrationLevel: ${ex.getMessage}")
    }

  def apply(typLevel: String): TYPLevel = {
    val (typ, calibrationLevel) = typLevel.splitAt(typLevel.length - 1)
    val level                   = parseCalibrationLevel(calibrationLevel)
    TYPLevel(TYP.withNameInsensitive(typ), level)
  }
}
