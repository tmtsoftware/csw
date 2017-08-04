package csw.units

import enumeratum.{Enum, EnumEntry}
import spray.json.JsonFormat

import scala.collection.immutable

sealed abstract class Units(name: String) extends EnumEntry with Serializable {

  // Should parameterize Units so concat can be created concat[A, B]
  override def toString = "[" + name + "]"

}

/**
 * This Units stuff is just for play
 * although something should be developed or borrowed
 * for use.
 */
object Units extends Enum[Units] {

  import csw.param.formats.JsonSupport._

  override def values: immutable.IndexedSeq[Units] = findValues
  implicit val format: JsonFormat[Units]           = enumFormat(this)

  object NoUnits extends Units("none")

  object encoder extends Units("enc")

  object micrometers extends Units("µm")

  object millimeters extends Units("mm")

  object meters extends Units("m")

  object kilometers extends Units("km")

  object degrees extends Units("deg")

  object seconds extends Units("sec")

  object milliseconds extends Units("ms")

}
