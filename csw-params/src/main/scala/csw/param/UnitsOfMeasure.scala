package csw.param

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
 * This Units stuff is just for play
 * although something should be developed or borrowed
 * for use.
 */
object UnitsOfMeasure extends DefaultJsonProtocol {

  // Should parameterize Units so concat can be created concat[A, B]
  case class Units(name: String) {
    override def toString = "[" + name + "]"
  }

  object NoUnits extends Units("none")

  object encoder extends Units("enc")

  object micrometers extends Units("µm")

  object millimeters extends Units("mm")

  object meters extends Units("m")

  object kilometers extends Units("km")

  object degrees extends Units("deg")

  object seconds extends Units("sec")

  object milliseconds extends Units("ms")

  object Units {
    def fromString(name: String): Units = name match {
      case encoder.name      => encoder
      case micrometers.name  => micrometers
      case millimeters.name  => millimeters
      case meters.name       => meters
      case kilometers.name   => kilometers
      case degrees.name      => degrees
      case seconds.name      => seconds
      case milliseconds.name => milliseconds
      case _                 => NoUnits
    }

    implicit val unitsFormat: RootJsonFormat[Units] = jsonFormat1(Units.apply)
  }
}
