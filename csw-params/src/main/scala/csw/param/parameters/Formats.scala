package csw.param.parameters
import csw.param.ParameterSetJson._
import spray.json.JsonFormat

object Formats {
  private var values: Map[String, JsonFormat[GParam[_]]] = Map.empty

  def get(typeName: String): Option[JsonFormat[GParam[_]]] = values.get(typeName)

  def register[S: JsonFormat](typeName: String): Unit = {
    val format = implicitly[JsonFormat[GParam[S]]].asInstanceOf[JsonFormat[GParam[_]]]
    values += (typeName → format)
  }
}
