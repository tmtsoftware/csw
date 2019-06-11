package csw.params.core.models

/*
 *  Copyright Jan Kotek 2009, http://asterope.org
 *  This program is distributed under GPL Version 3.0 in the hope that
 *  it will be useful, but WITHOUT ANY WARRANTY.
 */

import play.api.libs.json._

import scala.language.implicitConversions

/**
 * An wrapper for angle. Normally angle would be stored in double
 * as radians, but this introduces rounding errors.
 * This class stores value in microarc seconds to prevent rounding errors.
 * <p>
 * Usage examples:
 * <code>
 * //import provides implicit conversions for numbers
 * import Angle._
 * //use implicit conversions to construct angle from number
 * val angle = 10.degree + 0.5.arcSec
 * //convert value to radian an print it
 * println(11.toRadian)
 * </code>
 */
//noinspection ScalaStyle
case class Angle(uas: Long) extends AnyVal with Serializable with Ordered[Angle] {

  //require(uas> - Angle.CIRCLE && uas < Angle.CIRCLE, "out of range, possible overflow ");

  //operators
  def +(a2: Angle): Angle = new Angle(uas + a2.uas)

  def -(a2: Angle): Angle = new Angle(uas - a2.uas)

  def *(a2: Double): Angle = new Angle((uas * a2).toLong)

  def *(a2: Int): Angle = new Angle(uas * a2)

  def /(a2: Double): Angle = new Angle((uas / a2).toLong)

  def /(a2: Int): Angle = new Angle(uas / a2)

  def unary_+ : Angle = this

  def unary_- : Angle = Angle(-uas)

  override def compare(that: Angle): Int = uas.compare(that.uas)

  /** returns angle value in radians */
  def toRadian: Double = Angle.Uas2R * uas

  /** returns angle value in degrees */
  def toDegree: Double = Angle.Uas2D * uas

  /** returns angle value in mili arc seconds */
  def toMas: Double = uas * 1e-3

  /** returns angle value in arc seconds */
  def toArcSec: Double = 1e-6 * uas

  /** returns Angle with value normalized between 0 to 2*PI */
  def normalizedRa: Angle = {
    var uas2 = uas
    while (uas2 < 0) uas2 += Angle.CIRCLE
    uas2 = uas2 % Angle.CIRCLE

    new Angle(uas2)
  }

  override def toString: String = "Angle(" + toDegree + " degree)"

  /** Returns sequence of angles with given max value and increment */
  def to(maxVal: Angle, increment: Angle): Seq[Angle] = (uas to (maxVal.uas, increment.uas)).map(Angle(_))

  /** Returns sequence of angles with given max value and increment */
  def until(maxVal: Angle, increment: Angle): Seq[Angle] = (uas until (maxVal.uas, increment.uas)).map(Angle(_))
}

object Angle {

  protected[models] val CIRCLE: Long = 360L * 60L * 60L * 1000L * 1000L

  // Added for Play-Json support
  implicit val format: Format[Angle] = new Format[Angle] {
    override def writes(obj: Angle): JsValue           = JsNumber(obj.uas)
    override def reads(json: JsValue): JsResult[Angle] = JsSuccess(Angle(json.as[Long]))
  }

  /** used in implicit conversion to support `1.degree`, `1.arcMinute` etc */
  class AngleWrapperDouble(value: Double) {
    def degree: Angle = Angle((value * Angle.D2Uas).toLong)

    def arcMinute: Angle = Angle((value * Angle.M2Uas).toLong)

    def arcSec: Angle = Angle((value * Angle.S2Uas).toLong)

    def arcHour: Angle = Angle((value * Angle.H2Uas).toLong)

    def radian: Angle = Angle((value * Angle.R2Uas).toLong)

    def mas: Angle = Angle((value * 1000).toLong)
  }

  //implicit conversions
  implicit def long2angle(d: Long): AngleWrapperDouble = new AngleWrapperDouble(d)

  implicit def int2angle(d: Int): AngleWrapperDouble = new AngleWrapperDouble(d)

  implicit def double2angle(d: Double): AngleWrapperDouble = new AngleWrapperDouble(d)

  /** returns random angle with value between 0 and 2*PI */
  def randomRa(): Angle = new Angle((CIRCLE * math.random).asInstanceOf[Int])

  /** returns random angle with value between -PI/2 and + PI/2 */
  def randomDe(): Angle = new Angle((CIRCLE / 2 * math.random - CIRCLE / 4).asInstanceOf[Int])

  /** returns maximal angle from two options */
  def max(a1: Angle, a2: Angle): Angle = if (a1 > a2) a1 else a2

  /** returns minimal angle from two options */
  def min(a1: Angle, a2: Angle): Angle = if (a1 < a2) a1 else a2

  /**
   * Parse Declination from four values. It uses BigDecimal, so there are no rounding errors
   *
   * @param deSign   signum (ie + or -)
   * @param deDegree declination in degrees
   * @param deMin    remaining part in arc minutes
   * @param deSec    remaining part in arc seconds
   * @return declination in Micro Arc Seconds
   */
  def parseDe(deSign: String, deDegree: String, deMin: String, deSec: String): Angle = {
    val sign: Int = if ("-".equals(deSign.trim)) -1 else 1
    import java.math.BigDecimal
    val deg: BigDecimal = new BigDecimal(deDegree)
    if (deg.doubleValue < 0 || deg.doubleValue > 89) throw new IllegalArgumentException("Invalid deDegree: " + deg)
    val min: BigDecimal = if (deMin != null) new BigDecimal(deMin) else BigDecimal.ZERO
    if (min.doubleValue < 0 || min.doubleValue >= 60) throw new IllegalArgumentException("Invalid deMin: " + min)
    val sec: BigDecimal = if (deSec != null) new BigDecimal(deSec) else BigDecimal.ZERO
    if (sec.doubleValue < 0 || sec.doubleValue >= 60) throw new IllegalArgumentException("Invalid deSec: " + sec)

    Angle(
      sign *
        (deg.multiply(new BigDecimal(Angle.D2Uas)).longValueExact +
          min.multiply(new BigDecimal(Angle.M2Uas)).longValueExact +
          sec.multiply(new BigDecimal(Angle.S2Uas)).longValueExact)
    )
  }

  /**
   * Tries to parse Angle from string.
   * It knows common formats used for Declination
   */
  def parseDe(de: String): Angle = {
    if (de == null) throw new IllegalArgumentException("de is null")
    val r1 = ("([+|-]?)([0-9]+)[" + Angle.DEGREE_SIGN + "d: ]{1,2}([0-9]+)[m': ]{1,2}([0-9\\.]+)[s\\\"]?").r
    val r2 = ("([+|-]?)([0-9]+)[" + Angle.DEGREE_SIGN + "d: ]{1,2}([0-9]+)[m']?").r
    de match {
      case r1(sign, d, m, s) => parseDe(sign, d, m, s)
      case r2(sign, d, m)    => parseDe(sign, d, m, null)
      case _                 => throw new IllegalArgumentException("Could not parse DE: " + de)
    }
  }

  /**
   * parse Right ascencion  from triple values raHour raMin, raSec
   * This method uses big decimal, so there are no rounding errors
   *
   * @param raHour ra hours value as String
   * @param raMin ra minutes value as String
   * @param raSec ra seconds value as String
   * @return result in micro arc seconds
   */
  def parseRa(raHour: String, raMin: String, raSec: String): Angle = {
    import java.math.BigDecimal
    val raHour2: BigDecimal = new BigDecimal(raHour)
    if (raHour2.doubleValue < 0 || raHour2.doubleValue > 23) throw new IllegalArgumentException("Invalid raHour: " + raHour2)
    val raMin2: BigDecimal = if (raMin != null) new BigDecimal(raMin) else BigDecimal.ZERO
    if (raMin2.doubleValue < 0 || raMin2.doubleValue >= 60) throw new IllegalArgumentException("Invalid raMin: " + raMin2)
    val raSec2: BigDecimal = if (raSec != null) new BigDecimal(raSec) else BigDecimal.ZERO
    if (raSec2.doubleValue < 0 || raSec2.doubleValue >= 60) throw new IllegalArgumentException("Invalid raSec: " + raSec2)

    Angle(
      raHour2.multiply(new BigDecimal(Angle.H2Uas)).longValueExact +
        raMin2.multiply(new BigDecimal(Angle.HMin2Uas)).longValueExact +
        raSec2.multiply(new BigDecimal(Angle.HSec2Uas)).longValueExact
    )
  }

  /**
   * Tries to parse Angle from string.
   * It knows common formats used for Right ascencion (including hours)
   */
  def parseRa(ra: String): Angle = {
    if (ra == null) throw new IllegalArgumentException("ra is null")
    val r1 = "([0-9]+)[h: ]{1,2}([0-9]+)[m: ]{1,2}([0-9\\.]+)[s]{0,1}".r
    val r2 = "([0-9]+)[h: ]{1,2}([0-9\\.]+)[m]?".r
    val r3 = "([0-9]+)d([0-9]+)m([0-9\\.]+)s".r
    ra match {
      case r1(h, m, s) => parseRa(h, m, s)
      case r2(h, m)    => parseRa(h, m, null)
      case r3(d, m, s) => d.toDouble.degree + m.toDouble.arcMinute + s.toDouble.arcSec
      case _           => throw new IllegalArgumentException("Could not parse RA: " + ra)
    }
  }

  /**
   * Parses pair of RA and De coordinates.
   * This method should handle formats used in vizier.
   * An example:
   * The following writings are allowed:
   * <pre>
   * 20 54 05.689 +37 01 17.38
   * 10:12:45.3-45:17:50
   * 15h17m-11d10m
   * 15h17+89d15
   * 275d11m15.6954s+17d59m59.876s
   * 12.34567h-17.87654d
   * 350.123456d-17.33333d <=> 350.123456 -17.33333
   * </pre>
   */
  def parseRaDe(str: String): (Angle, Angle) = {
    //20 54 05.689 +37 01 17.38
    //10:12:45.3-45:17:50
    lazy val r1 = "([0-9]{2})[ :]([0-9]{2})[ :]([0-9]{2}\\.[0-9]+)[ ]?(\\+|-)([0-9]{2})[ :]([0-9]{2})[ :]([0-9]{2}\\.?[0-9]*)".r
    //15h17m-11d10m
    //15h17+89d15
    lazy val r2 = "([0-9]{2})h([0-9]{2})[m]?(\\+|-)([0-9]{2})d([0-9]{2})[m]?".r
    //275d11m15.6954s+17d59m59.876s
    lazy val r3 = "([0-9]{2,3}d[0-9]{2}m[0-9]{2}\\.[0-9]+s)([\\+-][0-9]{2}d[0-9]{2}m[0-9]{2}\\.[0-9]+s)".r
    //12.34567h-17.87654d
    lazy val r4 = "([0-9]{1,2}\\.[0-9]+)h([\\+-][0-9]{2}\\.[0-9]+)d".r
    //350.123456d-17.33333d <=> 350.123456 -17.33333
    lazy val r5 = "([0-9]{1,3}\\.?[0-9]*)[d]?[ ]?([\\+-]?[0-9]{1,2}\\.?[0-9]*)[d]?".r

    str match {
      case r1(ah, am, as, ss, d, m, s) => (parseRa(ah, am, as), parseDe(ss, d, m, s))
      case r2(ah, am, ss, d, m)        => (parseRa(ah, am, null), parseDe(ss, d, m, null))
      case r3(ra, de)                  => (parseRa(ra), parseDe(de))
      case r4(ra, de)                  => (ra.toDouble.arcHour, de.toDouble.degree)
      case r5(ra, de)                  => (ra.toDouble.degree, de.toDouble.degree)
    }

  }

  /**
   * normalize RA into 0 - 2 * PI range
   */
  def normalizeRa(ra2: Double): Double = {
    var ra = ra2
    while (ra < 0) ra += math.Pi * 2
    while (ra >= math.Pi * 2) ra -= math.Pi * 2

    ra
  }

  def assertRa(ra: Double): Unit = {
    if (ra < 0 || ra >= math.Pi * 2d)
      throw new IllegalArgumentException("Invalid RA: " + ra)
  }

  def assertDe(de: Double): Unit = {
    if (de < -Angle.D2R * 90d || de > Angle.D2R * 90d)
      throw new IllegalArgumentException("Invalid DE: " + de)
  }

  private def isNear(x: Double, d: Double): Boolean = {
    val tolerance = 1e-7
    math.abs(x % d) < tolerance || math.abs(x % d - d) < tolerance
  }

  private def formatSecs(sec: Double): String = {
    if (isNear(sec, 1))
      s"${math.round(sec)}"
    else if (isNear(sec, 0.1))
      f"$sec%2.1f"
    else if (isNear(sec, 0.01))
      f"$sec%2.2f"
    else
      s"$sec"
  }

  /**
   * convert RA to string in format '1h 2m'
   * minutes and seconds are auto added as needed
   *
   * @param ra in radians
   * @return ra in string form
   */
  def raToString(ra: Double): String = {
    if (isNear(ra, H2R)) {
      val hour = math.round(ra * R2H).toInt
      s"${hour}h"
    } else if (isNear(ra, H2R / 60)) {
      val hour = (ra * R2H).toInt
      val min  = Math.round((ra - H2R * hour) * R2H * 60).toInt
      s"${hour}h ${min}m"
    } else {
      val hour = (ra * R2H).toInt
      val min  = ((ra - H2R * hour) * R2H * 60).toInt
      val sec  = (ra - H2R * hour - min * H2R / 60) * R2H * 3600
      val s    = formatSecs(sec)
      s"${hour}h ${min}m ${s}s"
    }
  }

  /**
   * convert DE to string in format '1d 2m'
   * minutes and seconds are auto added as needed
   *
   * @param de2 in radians
   * @return de in string form
   */
  def deToString(de2: Double): String = {
    val (de, sign) = if (de2 < 0) (-de2, "-") else (de2, "")

    if (isNear(de, D2R)) {
      val deg = math.round(de * R2D).toInt
      sign + deg + DEGREE_SIGN
    } else if (isNear(de, M2R)) {
      val deg = (de * R2D).toInt
      val min = ((de - D2R * deg) * R2M).toInt
      sign + deg + DEGREE_SIGN + min + "'"
    } else {
      val deg = (de * R2D).toInt
      val min = ((de - D2R * deg) * R2D * 60).toInt
      val sec = (de - D2R * deg - min * D2R / 60) * R2D * 3600
      val s   = formatSecs(sec)
      sign + deg + DEGREE_SIGN + min + "'" + s + "\""
    }
  }

  /**
   * calculate great circle distance of two points,
   * coordinates are given in radians
   *
   * @return distance of two points in radians
   */
  def distance(ra1: Double, de1: Double, ra2: Double, de2: Double): Double = {
    //check ranges
    assertRa(ra1)
    assertRa(ra2)
    assertDe(de1)
    assertDe(de2)

    //this code is from JH labs projection lib
    val dlat = math.sin((de2 - de1) / 2)
    val dlon = math.sin((ra2 - ra1) / 2)
    val r    = math.sqrt(dlat * dlat + Math.cos(de1) * Math.cos(de2) * dlon * dlon)
    2.0 * math.asin(r)
  }

  /** multiply to convert degrees to radians */
  val D2R: Double = math.Pi / 180d

  /** multiply to convert radians to degrees */
  val R2D: Double = 1d / D2R

  /** multiply to convert degrees to arc hours */
  val D2H: Double = 1d / 15d

  /** multiply to convert arc hour to degrees */
  val H2D: Double = 1d / D2H

  /** multiply to convert degrees to arc minute */
  val D2M: Int = 60

  /** multiply to convert arc minute  to toDegree */
  val M2D: Double = 1d / D2M

  /** multiply to convert degrees to arc second */
  val D2S: Int = 3600

  /** multiply to convert arc second to toDegree */
  val S2D: Double = 1d / D2S

  /** multiply to convert hours to radians */
  val H2R: Double = Angle.H2D * Angle.D2R

  /** multiply to convert radians to hours */
  val R2H: Double = 1d / H2R

  /** multiply to convert radians to minutes */
  val R2M: Double = R2D * D2M

  /** multiply to convert minutes to radians */
  val M2R: Double = 1d / R2M

  /** multiply to convert milli arc seconds to radians */
  val Mas2R: Double = D2R / 3600000d

  /** multiply to convert micro arc seconds to radians */
  val Uas2R: Double = D2R / 3600000000d

  /** multiply to convert radians to mili arc seconds */
  val R2Mas: Double = 1d / Mas2R

  /** multiply to convert radians to micro arc seconds */
  val R2Uas: Double = 1d / Uas2R

  /** multiply to convert hours to mili arc seconds */
  val H2Mas: Int = 15 * 60 * 60 * 1000

  /** multiply to convert time minutes to mili arc seconds */
  val HMin2Mas: Int = 15 * 60 * 1000

  /** multiply to convert time seconds to mili arc seconds */
  val HSec2Mas: Int = 15 * 1000

  /** multiply to convert hours to micro arc seconds */
  val H2Uas: Long = 15L * 60L * 60L * 1000L * 1000L

  /** multiply to convert time minutes to micro arc seconds */
  val HMin2Uas: Long = 15L * 60L * 1000L * 1000L

  /** multiply to convert time seconds to micro arc seconds */
  val HSec2Uas: Long = 15L * 1000L * 1000L

  /** multiply to convert degrees to mili arc seconds */
  val D2Mas: Int = 60 * 60 * 1000

  /** multiply to convert minutes to mili arc seconds */
  val M2Mas: Int = 60 * 1000

  /** multiply to convert Seconds to mili arc seconds */
  val S2Mas: Int = 1000

  /** multiply to convert degrees to micro arc seconds */
  val D2Uas: Long = 60L * 60L * 1000L * 1000L

  /** multiply to convert minutes to micro arc seconds */
  val M2Uas: Long = 60L * 1000L * 1000L

  /** multiply to convert Seconds to micro arc seconds */
  val S2Uas: Long = 1000L * 1000L

  /** multiply to convert UAS to degrees  */
  val Uas2D: Double = 1d / D2Uas

  /** multiply to convert UAS to minutes  */
  val Uas2M: Double = 1d / M2Uas

  /** multiply to convert UAS to Seconds  */
  val Uas2S: Double = 1d / S2Uas

  /** multiply to convert  arc seconds to radians */
  val S2R: Double = D2R / 3600d

  /** multiply to convert radians to  arc seconds */
  val R2S: Double = 1d / S2R

  /** round circle which marks degrees */
  val DEGREE_SIGN: Char = '\u00B0'

}
