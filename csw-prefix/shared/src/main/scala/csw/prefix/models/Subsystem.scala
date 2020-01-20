package csw.prefix.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
 * Represents a TMT subsystem
 *
 * @param description of subsystem
 */
sealed abstract class Subsystem(description: String) extends EnumEntry with Serializable {

  /**
   * Represents a string with entryName and description of a subsystem
   */
  def longName: String = entryName + " - " + description

  /**
   * Represents the name of the subsystem in lowercase e.g NFIRAOS will be nfiraos
   */
  def name: String = entryName
}

/**
 * Defines constants for the available subsystems
 */
object Subsystem extends Enum[Subsystem] {

  override def values: immutable.IndexedSeq[Subsystem] = findValues

  case object AOESW   extends Subsystem("AO Executive Software")
  case object APS     extends Subsystem("Alignment and Phasing System")
  case object CIS     extends Subsystem("Communications and Information Systems")
  case object CLN     extends Subsystem("Mirror Cleaning System")
  case object CRYO    extends Subsystem("Cryogenic Cooling System")
  case object CSW     extends Subsystem("Common Software")
  case object DMS     extends Subsystem("Data Management System")
  case object DPS     extends Subsystem("Data Processing System")
  case object ESEN    extends Subsystem("Engineering Sensor System")
  case object ESW     extends Subsystem("Executive Software System")
  case object FMCS    extends Subsystem("Facility Management Control System")
  case object GMS     extends Subsystem("Global Metrology System Controls")
  case object IRIS    extends Subsystem("InfraRed Imaging Spectrometer")
  case object LGSF    extends Subsystem("Lasert Guide Star Facility")
  case object M1CS    extends Subsystem("M1 Control System")
  case object MODHIS  extends Subsystem("Multi-Object Diffraction-limited High-resolution Infrared Spectrograph")
  case object NFIRAOS extends Subsystem("Narrow Field Infrared AO System")
  case object NSCU    extends Subsystem("NFIRAOS Science Calibration Unit")
  case object OSS     extends Subsystem("Observatory Safety System")
  case object PFCS    extends Subsystem("Prime Focus Camera Controls")
  case object PSFR    extends Subsystem("NFIRAOS AO PSF Reconstructor")
  case object REFR    extends Subsystem("Refrigeration Control System")
  case object RTC     extends Subsystem("NFIRAOS Real-time Controller")
  case object RPG     extends Subsystem("NFIRAOS AO Reconstructor Parameter Generator")
  case object SCMS    extends Subsystem("Site Conditions Monitoring System")
  case object SOSS    extends Subsystem("Science Operations Support System")
  case object TCS     extends Subsystem("Telescope Control System")
  case object WFOS    extends Subsystem("Wide Field Optical Spectrometer")

  case object Container extends Subsystem("Container Subsystem")
}
