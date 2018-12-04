package csw.params.core.models

import csw.params.core.generics.SimpleKeyType
import play.api.libs.json.{Json, OFormat}

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

  val ICRS_C = Choice("ICRS")
  val FK4_C  = Choice("FK4")
  val FK5_C  = Choice("FK5")

  sealed trait EQ_FRAME {
    def toChoice: Choice
  }
  case object ICRS extends EQ_FRAME {
    val toChoice = ICRS_C
  }
  case object FK4 extends EQ_FRAME {
    val toChoice = FK4_C
  }
  case object FK5 extends EQ_FRAME {
    val toChoice = FK5_C
  }
  /*
  trait EqCoordinate {
    def tag: Tag
    def catalogName: String
    def frame: EQ_FRAME
    def ra: String
    def dec: String
    def pmx: Float
    def pmy: Float
    override def toString = s"EqCoordinate($tag,$catalogName,$frame,$ra,$dec,$pmx,$pmy)"
  }
   */


  case class EqCoordinate(ra: Angle, dec: Angle) {

    override def toString():String = Angle.raToString(ra.toRadian) + " " + Angle.deToString(dec.toRadian)
  }

  object EqCoordinate {
    // Converts Strings
    def apply(raStr: String, decStr: String): EqCoordinate = {
      val ra = Angle.parseRa(raStr)
      val dec = Angle.parseDe(decStr)
      new EqCoordinate(ra, dec)
    }

    // Converts to doubles each as degrees
    def apply(rad: Double, decd: Double): EqCoordinate = {
      val ra = Angle.double2angle(rad).degree
      val dec:Angle = Angle.double2angle(decd).degree
      new EqCoordinate(ra, dec)
    }

    def apply(radec: String): EqCoordinate = {
      val (ra, dec) = Angle.parseRaDe(radec)
      new EqCoordinate(ra, dec)
    }

    //used by play-json
    implicit val eqFormat: OFormat[EqCoordinate] = Json.format[EqCoordinate]

    /*
    def apply(tag: Tag = BASE,
              catalogName: String = "none",
              frame: EQ_FRAME = ICRS,
              pmx: Float = 0.0f,
              pmy: Float = 0.0f): EqCoordinate = new EqCoordinateImpl(tag, catalogName, frame, "1.0", "2.0", pmx, pmy)
   */
  }
  case object EqCoordinateKey extends SimpleKeyType[EqCoordinate]

  implicit val tagFormat: OFormat[Tag] = Json.format[Tag]

  //used by play-json
 // implicit val eqCoordinateFormat: OFormat[EqCoordinate] = Json.format[EqCoordinate]

}
/*
val catNameKey = KeyType.StringKey.make("catalogName") // catalog name
  val longKey = KeyType.StringKey.make("ra") // long = ra for equatorial-based frames
  val latKey = KeyType.StringKey.make("dec") // lat = dec "" -- not sure this is necessary with this model
  val epochKey = KeyType.DoubleKey.make("epoch")
val equinoxKey = KeyType.DoubleKey.make("equinox")
val pmxKey = KeyType.FloatKey.make("pmx")
val pmyKey = KeyType.FloatKey.make("pmy")
  val parallaxKey = KeyType.FloatKey.make(name = "parallax")
  val velocityKey = KeyType.FloatKey.make(name = "rv")
 */
// Equitorial coordinate frame

// val frameKey = ChoiceKey.make("frame", ICRS_C, FK4_C, FK5_C)
