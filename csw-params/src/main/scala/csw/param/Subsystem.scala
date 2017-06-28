package csw.param

import scala.collection.JavaConverters._

/**
 * Represents a TMT subsystem
 */
sealed abstract class Subsystem(val name: String, val prefix: String, val description: String)
    extends Ordered[Subsystem]
    with Serializable {

  override def toString = name

  def longName = name + " - " + description

  def compare(that: Subsystem) = name.compare(that.name)
}

/**
 * Defines constants for the available subsystems
 */
object Subsystem {

  case object AOESW   extends Subsystem("AOESW", "aoesw", "AO Executive Software")
  case object APS     extends Subsystem("APS", "aps", "Alignment and Phasing System")
  case object CIS     extends Subsystem("CIS", "cis", "Communications and Information Systems")
  case object CSW     extends Subsystem("CSW", "csw", "Common Software")
  case object DMS     extends Subsystem("DMS", "dms", "Data Management System")
  case object DPS     extends Subsystem("DPS", "dps", "Data Processing System")
  case object ENC     extends Subsystem("ENC", "enc", "Enclosure")
  case object ESEN    extends Subsystem("ESEN", "esen", "Engineering Sensor System")
  case object ESW     extends Subsystem("ESW", "esw", "Executive Software System")
  case object GMS     extends Subsystem("GMS", "gms", "Global Metrology System Controls")
  case object IRIS    extends Subsystem("IRIS", "iris", "InfraRed Imaging Spectrometer")
  case object IRMS    extends Subsystem("IRMS", "irms", "Infrared Multi-Slit Spectrometer")
  case object LGSF    extends Subsystem("LGSF", "lgsf", "Lasert Guide Star Facility")
  case object M1CS    extends Subsystem("M1CS", "m1cs", "M1 Control System")
  case object M2CS    extends Subsystem("M2CS", "m2cs", "M2 Control System")
  case object M3CS    extends Subsystem("M3CS", "m3cs", "M3 Control System")
  case object MCS     extends Subsystem("MCS", "mcs", "Mount Control System")
  case object NFIRAOS extends Subsystem("NFIRAOS", "nfiraos", "Narrow Field Infrared AO System")
  case object NSCU    extends Subsystem("NSCU", "nscu", "NFIRAOS Science Calibration Unit")
  case object OSS     extends Subsystem("OSS", "oss", "Observatory Safety System")
  case object PFCS    extends Subsystem("PFCS", "pfcs", "Prime Focus Camera Controls")
  case object PSFR    extends Subsystem("PSFR", "psfr", "NFIRAOS AO PSF Reconstructor")
  case object RTC     extends Subsystem("RTC", "rtc", "NFIRAOS Real-time Controller")
  case object RPG     extends Subsystem("RPG", "rpg", "NFIRAOS AO Reconstructor Parameter Generator")
  case object SCMS    extends Subsystem("SCMS", "scms", "Site Conditions Monitoring System")
  case object SOSS    extends Subsystem("SOSS", "soss", "Science Operations Support System")
  case object STR     extends Subsystem("STR", "str", "Telescope Structure")
  case object SUM     extends Subsystem("SUM", "sum", "Summit Facility")
  case object TCS     extends Subsystem("TCS", "tcs", "Telescope Control System")
  case object TINC    extends Subsystem("TINC", "tinc", "Prime Focus Camera Controls")
  case object WFOS    extends Subsystem("WFOS", "wfos", "Wide Field Optical Spectrometer")
  // for testing
  case object TEST extends Subsystem("TEST", "test", "Testing System")
  case object BAD  extends Subsystem("BAD", "bad", "Unknown/default Subsystem ")

  val subsystems: Set[Subsystem] = Set(AOESW, APS, CIS, CSW, DMS, DPS, ENC, ESEN, ESW, GMS, IRIS, IRMS, LGSF, M1CS,
    M2CS, M3CS, MCS, NFIRAOS, NSCU, OSS, PFCS, PSFR, RTC, RPG, SCMS, SOSS, STR, SUM, TCS, TINC, WFOS)

  val jSubsystems: java.util.Set[Subsystem] = subsystems.asJava

  def lookup(in: String): Option[Subsystem] = subsystems.find(sub => sub.prefix.equalsIgnoreCase(in))
}
