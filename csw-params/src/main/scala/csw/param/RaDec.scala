package csw.param

import csw.param.parameters.GParam
import spray.json.{DefaultJsonProtocol, DeserializationException, JsArray, JsString, JsValue}

/**
 * Holds ra and dec values
 */
case class RaDec(ra: Double, dec: Double)

/**
 * Since automatic JSON reading doesn't work with generic types, we need to do it manually here.
 */
case object RaDec extends DefaultJsonProtocol {

  // Name used as type key in JSON, and for registering the reader: Must be unique
  private val typeName = "RaDec" // XXX Should use full path name here?

  /**
   * JSON read/write for RaDecItem
   */
  implicit val raDecFormat = jsonFormat2(RaDec.apply)

  /**
   * Creates a GenericItem[RaDec] from a JSON value (This didn't work with the jsonFormat3 method)
   */
  def reader(json: JsValue): GParam[RaDec] = {
    json.asJsObject.getFields("keyName", "value", "units") match {
      case Seq(JsString(keyName), JsArray(v), u) =>
        val units = ParameterSetJson.unitsFormat.read(u)
        val value = v.map(RaDec.raDecFormat.read)
        GParam[RaDec](typeName, keyName, value.toArray, units)
      case _ => throw new DeserializationException("Color expected")
    }
  }

  GParam.register(typeName, reader)
}
