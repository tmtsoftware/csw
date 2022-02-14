package csw.params.core.models
import csw.params.core.models.Coords.EqFrame.ICRS
import csw.params.core.models.Coords._
import enumeratum._

import scala.collection.immutable

object Coords {

  /**
   * A tag is a label to indicate the use of the coordinate
   *
   * @param name what is the role of this coordinate
   */
  case class Tag(name: String) {
    override def toString: String = name
  }
  val BASE    = Tag("BASE")
  val OIWFS1  = Tag("OIWFS1")
  val OIWFS2  = Tag("OIWFS2")
  val OIWFS3  = Tag("OIWFS3")
  val OIWFS4  = Tag("OIWFS4")
  val ODGW1   = Tag("ODGW1")
  val ODGW2   = Tag("ODGW2")
  val ODGW3   = Tag("ODGW3")
  val ODGW4   = Tag("ODGW4")
  val GUIDER1 = Tag("GUIDER1")
  val GUIDER2 = Tag("GUIDER2")

  sealed trait EqFrame extends EnumEntry
  object EqFrame extends Enum[EqFrame] {
    override def values: immutable.IndexedSeq[EqFrame] = findValues
    case object ICRS extends EqFrame
    case object FK5  extends EqFrame
  }

  /**
   * All coordinates are a Coord.
   * A Coord has a tag.
   */
  sealed trait Coord {
    val tag: Tag
  }

  case class AltAzCoord(tag: Tag, alt: Angle, az: Angle) extends Coord {
    override def toString: String = s"AltAzCoord($tag ${alt.toDegree}  ${az.toDegree})"
  }

  sealed trait SolarSystemObject extends EnumEntry
  object SolarSystemObject extends Enum[SolarSystemObject] {

    override def values: immutable.IndexedSeq[SolarSystemObject] = findValues

    case object Mercury extends SolarSystemObject
    case object Venus   extends SolarSystemObject
    case object Moon    extends SolarSystemObject
    case object Mars    extends SolarSystemObject
    case object Jupiter extends SolarSystemObject
    case object Saturn  extends SolarSystemObject
    case object Neptune extends SolarSystemObject
    case object Uranus  extends SolarSystemObject
    case object Pluto   extends SolarSystemObject
  }

  case class SolarSystemCoord(tag: Tag, body: SolarSystemObject) extends Coord

  case class MinorPlanetCoord(
      tag: Tag,
      epoch: Double,            // TT as a Modified Julian Date
      inclination: Angle,       // degrees
      longAscendingNode: Angle, // degrees
      argOfPerihelion: Angle,   // degrees
      meanDistance: Double,     // AU
      eccentricity: Double,
      meanAnomaly: Angle // degrees
  ) extends Coord

  case class CometCoord(
      tag: Tag,
      epochOfPerihelion: Double,  // TT as a Modified Julian Date
      inclination: Angle,         // degrees
      longAscendingNode: Angle,   // degrees
      argOfPerihelion: Angle,     // degrees
      perihelionDistance: Double, // AU
      eccentricity: Double
  ) extends Coord

  import EqCoord._

  /**
   * Equatorial coordinates.
   *
   * @param tag a Tag instance (name for the coordinates)
   * @param ra right ascension, expressed as an Angle instance
   * @param dec declination, expressed as an Angle instance
   * @param frame the IAU celestial reference system
   * @param catalogName  the name of the catalog from which the coordinates were taken (use "none" if unknown)
   * @param pm proper motion
   */
  case class EqCoord(tag: Tag, ra: Angle, dec: Angle, frame: EqFrame, catalogName: String, pm: ProperMotion) extends Coord {

    /**
     * Creates an EqCoord from the given arguments, which all have default values.
     * The values for ra and dec may be an Angle instance, or a String that can be parsed by Angle.parseRa()
     *
     * @param ra may be an Angle instance, or a String (in hms) that can be parsed by Angle.parseRa() or a Double value in degrees (default: 0.0)
     * @param dec may be an Angle instance, or a String that can be parsed by Angle.parseDe() or a Double value in degrees  (default: 0.0)
     * @param frame the the IAU celestial reference system (default: ICRS)
     * @param tag a Tag instance (name for the coordinates, default: "BASE")
     * @param catalogName the name of the catalog from which the coordinates were taken (default: "none")
     * @param pmx proper motion X coordinate (default: 0.0)
     * @param pmy proper motion y coordinate (default: 0.0)
     */
    def this(
        ra: Any = 0.0,
        dec: Any = 0.0,
        frame: EqFrame = DEFAULT_FRAME,
        tag: Tag = DEFAULT_TAG,
        catalogName: String = DEFAULT_CATNAME,
        pmx: Double = DEFAULT_PMX,
        pmy: Double = DEFAULT_PMY
    ) = {
      this(
        tag,
        ra match {
          case ras: String => Angle.parseRa(ras)
          case rad: Double => Angle.double2angle(rad).degree
          case raa: Angle  => raa
          case x           => throw new MatchError(x)
        },
        dec match {
          case des: String => Angle.parseDe(des)
          case ded: Double => Angle.double2angle(ded).degree
          case dea: Angle  => dea
          case x           => throw new MatchError(x)
        },
        frame,
        catalogName,
        ProperMotion(pmx, pmy)
      )
    }

    def withPM(pmx: Double, pmy: Double): EqCoord = this.copy(pm = ProperMotion(pmx, pmy))

    override def toString: String =
      s"EqCoord(${Angle.raToString(ra.toRadian)}  ${Angle.deToString(dec.toRadian)}" +
        s" ${frame.toString} $catalogName $tag ${pm.toString})"

  }

  object EqCoord {
    val DEFAULT_FRAME: EqFrame  = ICRS
    val DEFAULT_TAG: Tag        = BASE
    val DEFAULT_PMX: Double     = ProperMotion.DEFAULT_PROPERMOTION.pmx
    val DEFAULT_PMY: Double     = ProperMotion.DEFAULT_PROPERMOTION.pmy
    val DEFAULT_CATNAME: String = "none"

    /**
     * Creates an EqCoord from the given arguments, which all have default values.
     * See matching constructior for a description of the arguments.
     */
    def apply(
        ra: Any = 0.0,
        dec: Any = 0.0,
        frame: EqFrame = DEFAULT_FRAME,
        tag: Tag = DEFAULT_TAG,
        catalogName: String = DEFAULT_CATNAME,
        pmx: Double = DEFAULT_PMX,
        pmy: Double = DEFAULT_PMY
    ): EqCoord = new EqCoord(ra, dec, frame, tag, catalogName, pmx, pmy)

    /**
     * This allows creation of an EqCoordinate from a string of ra and dec with formats:
     * 20 54 05.689 +37 01 17.38
     * 10:12:45.3-45:17:50
     * 15h17m-11d10m
     * 15h17+89d15
     * 275d11m15.6954s+17d59m59.876s
     * 12.34567h-17.87654d
     *
     * @param radec string includes both the ra a dec continuous
     * @return a new EqCoord
     */
    def asBoth(
        radec: String,
        frame: EqFrame = DEFAULT_FRAME,
        tag: Tag = DEFAULT_TAG,
        catalogName: String = DEFAULT_CATNAME,
        pmx: Double = DEFAULT_PMX,
        pmy: Double = DEFAULT_PMY
    ): EqCoord = {
      val (ra, dec) = Angle.parseRaDe(radec)
      apply(tag, ra, dec, frame, catalogName, ProperMotion(pmx, pmy))
    }
  }
}

/**
 * For the Java API
 */
object JCoords {
  val ICRS: EqFrame = EqFrame.ICRS
  val FK5: EqFrame  = EqFrame.FK5

  val DEFAULT_FRAME: EqFrame  = ICRS
  val DEFAULT_TAG: Tag        = BASE
  val DEFAULT_PMX: Double     = ProperMotion.DEFAULT_PROPERMOTION.pmx
  val DEFAULT_PMY: Double     = ProperMotion.DEFAULT_PROPERMOTION.pmy
  val DEFAULT_CATNAME: String = "none"

  val Mercury: SolarSystemObject = SolarSystemObject.Mercury
  val Venus: SolarSystemObject   = SolarSystemObject.Venus
  val Moon: SolarSystemObject    = SolarSystemObject.Moon
  val Mars: SolarSystemObject    = SolarSystemObject.Mars
  val Jupiter: SolarSystemObject = SolarSystemObject.Jupiter
  val Saturn: SolarSystemObject  = SolarSystemObject.Saturn
  val Neptune: SolarSystemObject = SolarSystemObject.Neptune
  val Uranus: SolarSystemObject  = SolarSystemObject.Uranus
  val Pluto: SolarSystemObject   = SolarSystemObject.Pluto

}

object JEqCoord {
  def make(ra: Any, dec: Any): EqCoord = EqCoord(ra, dec)

  def asBoth(radec: String, frame: EqFrame, tag: Tag, catalogName: String, pmx: Double, pmy: Double): EqCoord =
    EqCoord.asBoth(radec, frame, tag, catalogName, pmx, pmy)
}
