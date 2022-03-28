/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

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

  case object AOESW   extends Subsystem("Adaptive Optics Executive Software")
  case object APS     extends Subsystem("Alignment and Phasing System ")
  case object CIS     extends Subsystem("Communications and Information Systems")
  case object CLN     extends Subsystem("Optical Cleaning Systems")
  case object CRYO    extends Subsystem("Instrumentation Cryogenic Cooling System")
  case object CSW     extends Subsystem("Common Software")
  case object DMS     extends Subsystem("Data Management System")
  case object DPS     extends Subsystem("Data Processing System")
  case object ENC     extends Subsystem("Enclosure")
  case object ESEN    extends Subsystem("Engineering Sensors")
  case object ESW     extends Subsystem("Executive Software")
  case object HNDL    extends Subsystem("Optics Handling Equipment")
  case object HQ      extends Subsystem("Observatory Headquarters")
  case object IRIS    extends Subsystem("InfraRed Imaging Spectrometer")
  case object LGSF    extends Subsystem("Laser Guide Star Facility")
  case object M1COAT  extends Subsystem("M1COAT M1 Optical Coating System")
  case object M1CS    extends Subsystem("M1CS M1 Control System ")
  case object M1S     extends Subsystem("M1S M1 Optics System")
  case object M2COAT  extends Subsystem("M2/M3 Optical Coating System")
  case object M2S     extends Subsystem("M2S M2 System")
  case object M3S     extends Subsystem("M3S M3 System")
  case object MODHIS  extends Subsystem("Multi-Object Diffraction-limited High-resolution IR Spectrograph")
  case object NFIRAOS extends Subsystem("Narrow Field Infrared AO System")
  case object OSS     extends Subsystem("Observatory Safety System")
  case object REFR    extends Subsystem("Instrumentation Refrigerant Cooling System ")
  case object SCMS    extends Subsystem("Site Conditions Monitoring System")
  case object SER     extends Subsystem("Services")
  case object SOSS    extends Subsystem("Science Operations Support Systems")
  case object STR     extends Subsystem("Structure ")
  case object SUM     extends Subsystem("Summit Facilities")
  case object TCS     extends Subsystem("Telescope Control System")
  case object TINS    extends Subsystem("Test Instruments")
  case object WFOS    extends Subsystem("Wide Field Optical Spectrograph")

  case object Container extends Subsystem("Container Subsystem")

}
