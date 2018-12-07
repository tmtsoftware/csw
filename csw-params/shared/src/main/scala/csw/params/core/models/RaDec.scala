package csw.params.core.models

import csw.params.core.generics.SimpleKeyType
import play.api.libs.json._

/**
 * Holds Ra(Right Ascension) and Dec(Declination) values
 */
case class RaDec(ra: Double, dec: Double)

case object RaDec {

  //used by play-json
  implicit val raDecFormat: OFormat[RaDec] = Json.format[RaDec]
}

object PositionsHelpers {

  case class Tag(tag: String) {
    override def toString = tag.toString
  }
  val BASE   = Tag("base")
  val OIWFS1 = Tag("oiwfs1")
  val OIWFS2 = Tag("oiwfs2")
  val OIWFS3 = Tag("oiwfs3")
  val GUIDE1 = Tag("quide1")
  val GUIDE2 = Tag("quide2")
  val GUIDE3 = Tag("quide3")

  implicit val tagFormat: OFormat[Tag] = Json.format[Tag]


  trait EQ_FRAME
  case object ICRS extends EQ_FRAME
  case object FK4 extends EQ_FRAME
  case object FK5 extends EQ_FRAME

  object EQ_FRAME {
    implicit val format: Format[EQ_FRAME] = new Format[EQ_FRAME] {
      override def writes(obj: EQ_FRAME): JsValue = JsString(obj.toString)
      override def reads(json: JsValue): JsResult[EQ_FRAME] = {
        val x:String = json.as[String]
        val rr = x match {
          case "ICRS" => ICRS
          case "FK4" => FK4
          case "FK5" => FK5
          case _ => ICRS
        }
        JsSuccess(rr)
      }
    }
  }

  case class ProperMotion(pmx: Double, pmy: Double) {
    override def toString: String = s"$pmx/$pmy"
  }

  trait Coordinate {
    def tag: Tag
  }

  object Coordinate {
    implicit val format: Format[Coordinate] = new Format[Coordinate] {
      override def writes(obj: Coordinate): JsValue = JsString(obj.tag.toString)
      override def reads(json: JsValue): JsResult[Coordinate] = {
        val x:String = json.as[String]
        JsSuccess(EqCoordinate(ra = 15.0, dec = 60.0))
      }
    }
  }

  case class EqCoordinate(tag: Tag, ra: Angle, dec: Angle, frame: EQ_FRAME, catalogName: String, pm: ProperMotion) extends Coordinate {

    def withPM(pmx: Double, pmy:Double): EqCoordinate = this.copy(pm = ProperMotion(pmx, pmy))

    override def toString(): String = s"${Angle.raToString(ra.toRadian)}  ${Angle.deToString(dec.toRadian)}" +
      s" ${frame.toString} $catalogName ${tag} ${pm.toString}"
  }

  case class AltAzCoordinate(tag: Tag, alt: Angle, az: Angle) {
    override def toString(): String = s"${alt.toDegree}  ${az.toDegree}"
  }

  object ProperMotion {
    val DEFAULT_PROPERMOTION = ProperMotion(0.0, 0.0)
    //used by play-json
    implicit val pmFormat: OFormat[ProperMotion] = Json.format[ProperMotion]
  }

  object EqCoordinate {

    val DEFAULT_FRAME = ICRS
    val DEFAULT_TAG = BASE
    val DEFAULT_PMX = ProperMotion.DEFAULT_PROPERMOTION.pmx
    val DEFAULT_PMY = ProperMotion.DEFAULT_PROPERMOTION.pmy
    val DEFAULT_CATNAME = "none"

    def apply(ra: Any = 0.0, dec: Any = 0.0, frame: EQ_FRAME = DEFAULT_FRAME, tag:Tag = DEFAULT_TAG,
              catalogName: String = DEFAULT_CATNAME,
              pmx:Double = DEFAULT_PMX, pmy:Double = DEFAULT_PMY): EqCoordinate = {
      val raAngle:Angle = ra match {
        case ras:String => Angle.parseRa(ras)
        case rad:Double => Angle.double2angle(rad).degree
        case raa:Angle => raa
      }
      val decAngle:Angle = dec match {
        case des:String => Angle.parseDe(des)
        case ded:Double => Angle.double2angle(ded).degree
        case dea:Angle => dea
      }
      new EqCoordinate(tag, raAngle, decAngle, ICRS, catalogName, ProperMotion(pmx,pmy))
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
     * @param radec
     * @return
     */
    def asBoth(radec: String, frame: EQ_FRAME = DEFAULT_FRAME, tag:Tag = DEFAULT_TAG,
               catalogName: String = DEFAULT_CATNAME,
               pmx:Double = DEFAULT_PMX, pmy:Double = DEFAULT_PMY): EqCoordinate = {
      val (ra, dec) = Angle.parseRaDe(radec)
      apply(tag, ra, dec, frame, catalogName, ProperMotion(pmx, pmy))
    }

    //used by play-json
    implicit val eqFormat: OFormat[EqCoordinate] = Json.format[EqCoordinate]


  }

  case object EqCoordinateKey extends SimpleKeyType[EqCoordinate]

  case object CoordinateKey extends SimpleKeyType[Coordinate]

}