package csw.messages.params.models

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import scala.collection.immutable

/**
 * Represents a TMT subsystem
 */
sealed abstract class Subsystem(description: String) extends EnumEntry with Lowercase with Serializable {
  def longName: String = entryName + " - " + description
}

/**
 * Defines constants for the available subsystems
 */
object Subsystem extends Enum[Subsystem] with PlayJsonEnum[Subsystem] {

  override def values: immutable.IndexedSeq[Subsystem] = findValues

  case object AOESW   extends Subsystem("AO Executive Software")
  case object APS     extends Subsystem("Alignment and Phasing System")
  case object CIS     extends Subsystem("Communications and Information Systems")
  case object CSW     extends Subsystem("Common Software")
  case object DMS     extends Subsystem("Data Management System")
  case object DPS     extends Subsystem("Data Processing System")
  case object ENC     extends Subsystem("Enclosure")
  case object ESEN    extends Subsystem("Engineering Sensor System")
  case object ESW     extends Subsystem("Executive Software System")
  case object GMS     extends Subsystem("Global Metrology System Controls")
  case object IRIS    extends Subsystem("InfraRed Imaging Spectrometer")
  case object IRMS    extends Subsystem("Infrared Multi-Slit Spectrometer")
  case object LGSF    extends Subsystem("Lasert Guide Star Facility")
  case object M1CS    extends Subsystem("M1 Control System")
  case object M2CS    extends Subsystem("M2 Control System")
  case object M3CS    extends Subsystem("M3 Control System")
  case object MCS     extends Subsystem("Mount Control System")
  case object NFIRAOS extends Subsystem("Narrow Field Infrared AO System")
  case object NSCU    extends Subsystem("NFIRAOS Science Calibration Unit")
  case object OSS     extends Subsystem("Observatory Safety System")
  case object PFCS    extends Subsystem("Prime Focus Camera Controls")
  case object PSFR    extends Subsystem("NFIRAOS AO PSF Reconstructor")
  case object RTC     extends Subsystem("NFIRAOS Real-time Controller")
  case object RPG     extends Subsystem("NFIRAOS AO Reconstructor Parameter Generator")
  case object SCMS    extends Subsystem("Site Conditions Monitoring System")
  case object SOSS    extends Subsystem("Science Operations Support System")
  case object STR     extends Subsystem("Telescope Structure")
  case object SUM     extends Subsystem("Summit Facility")
  case object TCS     extends Subsystem("Telescope Control System")
  case object TINC    extends Subsystem("Prime Focus Camera Controls")
  case object WFOS    extends Subsystem("Wide Field Optical Spectrometer")
  // for testing
  case object TEST extends Subsystem("Testing System")
  case object BAD  extends Subsystem("Unknown/default Subsystem")
}
