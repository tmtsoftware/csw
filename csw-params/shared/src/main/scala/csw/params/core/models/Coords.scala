package csw.params.core.models
import julienrf.json.derived
import play.api.libs.json.{Json, OFormat}

object Coords {

  /**
   * A tag is a label to indicate the use of the coordinate
   * @param name what is the role of this coordinate
   */
  case class Tag(name: String) {
    override def toString: String = name
  }
  val BASE         = Tag("BASE")
  val OIWFS1       = Tag("OIWFS1")
  val OIWFS2       = Tag("OIWFS2")
  val OIWFS3       = Tag("OIWFS3")
  val OIWFS4       = Tag("OIWFS4")
  val ODGW1        = Tag("ODGW1")
  val ODGW2        = Tag("ODGW2")
  val ODGW3        = Tag("ODGW3")
  val ODGW4        = Tag("ODGW4")
  val GUIDER1      = Tag("GUIDER1")
  val GUIDER2      = Tag("GUIDER2")
  val allTags      = Set(BASE, OIWFS1, OIWFS2, OIWFS3, OIWFS4, ODGW1, ODGW2, ODGW3, ODGW4, GUIDER1, GUIDER2)
  val allTagsNames = allTags.map(_.name)

  implicit val tagFormat: OFormat[Tag] = Json.format[Tag]

  sealed trait EQ_FRAME
  case object ICRS extends EQ_FRAME
  case object FK5  extends EQ_FRAME

  implicit val eqfFormat: OFormat[EQ_FRAME] = derived.oformat()
  //implicit val eqfFormat: OFormat[EQ_FRAME]      = flat.oformat((__ \ "frame").format[String])

  sealed trait Coord {
    val tag: Tag
  }

  object Coord {
    implicit val jsonFormat: OFormat[Coord] = derived.oformat()
    //implicit val jsonFormat: OFormat[Coord]      = derived.flat.oformat((__ \ "type").format[String])
  }

  case class AltAzCoord(tag: Tag, alt: Angle, az: Angle) extends Coord {
    override def toString: String = s"$tag ${alt.toDegree}  ${az.toDegree}"
  }
  object AltAzCoord {
    implicit val coordFormat: OFormat[AltAzCoord] = Json.format[AltAzCoord]
  }

  sealed trait SolarSystemObject
  case object Mercury extends SolarSystemObject
  case object Venus   extends SolarSystemObject
  case object Moon    extends SolarSystemObject
  case object Mars    extends SolarSystemObject
  case object Jupiter extends SolarSystemObject
  case object Saturn  extends SolarSystemObject
  case object Neptune extends SolarSystemObject
  case object Uranus  extends SolarSystemObject
  case object Pluto   extends SolarSystemObject

  object SolarSystemObject {
    implicit val ssoFormat: OFormat[SolarSystemObject] = derived.oformat()
  }

  case class SolarSystemCoord(tag: Tag, body: SolarSystemObject) extends Coord
  object SolarSystemCoord {
    implicit val coordFormat: OFormat[SolarSystemCoord] = Json.format[SolarSystemCoord]
  }

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
  object MinorPlanetCoord {
    implicit val coordFormat: OFormat[MinorPlanetCoord] = Json.format[MinorPlanetCoord]
  }
  /*
  case class CometCoord(tag: Tag,
                        epochOfPerihelion: Double,  // TT as a Modified Julian Date
                        inclination: Angle, // degrees
                        longAscendingNode: Angle, // degrees
                        argOfPerihelion: Angle, // degrees
                        perihelionDistance: Double, // AU
                        eccentricity: Double
                       ) extends Coord
  object CometCoord {
    implicit val coordFormat: OFormat[CometCoord] = Json.format[CometCoord]
  }

   */

  case class EqCoord(tag: Tag, ra: Angle, dec: Angle, frame: EQ_FRAME, catalogName: String, pm: ProperMotion) extends Coord {

    def withPM(pmx: Double, pmy: Double): EqCoord = this.copy(pm = ProperMotion(pmx, pmy))

    override def toString: String =
      s"EqCoord(${Angle.raToString(ra.toRadian)}  ${Angle.deToString(dec.toRadian)}" +
        s" ${frame.toString} $catalogName $tag ${pm.toString})"

  }

  object EqCoord {
    val DEFAULT_FRAME: EQ_FRAME = ICRS
    val DEFAULT_TAG: Tag        = BASE
    val DEFAULT_PMX: Double     = ProperMotion.DEFAULT_PROPERMOTION.pmx
    val DEFAULT_PMY: Double     = ProperMotion.DEFAULT_PROPERMOTION.pmy
    val DEFAULT_CATNAME: String = "none"

    def apply(
        ra: Any = 0.0,
        dec: Any = 0.0,
        frame: EQ_FRAME = DEFAULT_FRAME,
        tag: Tag = DEFAULT_TAG,
        catalogName: String = DEFAULT_CATNAME,
        pmx: Double = DEFAULT_PMX,
        pmy: Double = DEFAULT_PMY
    ): EqCoord = {
      val raAngle: Angle = ra match {
        case ras: String => Angle.parseRa(ras)
        case rad: Double => Angle.double2angle(rad).degree
        case raa: Angle  => raa
      }
      val decAngle: Angle = dec match {
        case des: String => Angle.parseDe(des)
        case ded: Double => Angle.double2angle(ded).degree
        case dea: Angle  => dea
      }
      new EqCoord(tag, raAngle, decAngle, ICRS, catalogName, ProperMotion(pmx, pmy))
    }

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
        frame: EQ_FRAME = DEFAULT_FRAME,
        tag: Tag = DEFAULT_TAG,
        catalogName: String = DEFAULT_CATNAME,
        pmx: Double = DEFAULT_PMX,
        pmy: Double = DEFAULT_PMY
    ): EqCoord = {
      val (ra, dec) = Angle.parseRaDe(radec)
      apply(tag, ra, dec, frame, catalogName, ProperMotion(pmx, pmy))
    }

    //used by play-json
    implicit val coordFormat: OFormat[EqCoord] = Json.format[EqCoord]
  }
}
