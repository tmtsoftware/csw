package csw.param.parameters
import spray.json.JsonFormat

import scala.reflect.ClassTag

object Formats {
  private var values: Map[String, JsonFormat[GParam[_]]] = Map.empty

  def get(typeName: String): Option[JsonFormat[GParam[_]]] = values.get(typeName)

  def register[S: JsonFormat: ClassTag](typeName: String): Unit = {

    val format = GParam[S].asInstanceOf[JsonFormat[GParam[_]]]
    values += (typeName → format)
  }
}
