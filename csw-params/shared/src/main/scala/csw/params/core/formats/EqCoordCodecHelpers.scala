package csw.params.core.formats

import csw.params.core.models.{Angle, Coords, ProperMotion}
import csw.params.core.models.Coords.{EqCoord, EqFrame}

object EqCoordCodecHelpers {
  case class WireModel(tag: Coords.Tag, ra: String, dec: String, frame: EqFrame, catalogName: String, pm: ProperMotion)

  def toEqCord(wireModel: WireModel): EqCoord = {
    import wireModel.*
    EqCoord(tag, Angle.parseRa(ra), Angle.parseDe(dec), frame, catalogName, pm)
  }

  def fromEqCord(eqCoord: EqCoord): WireModel = {
    import eqCoord.*
    WireModel(tag, Angle.raToString(ra.toRadian), Angle.deToString(dec.toRadian), frame, catalogName, pm)
  }
}
