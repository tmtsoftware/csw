package csw.messages.framework

import ai.x.play.json.Jsonx
import csw.messages.TMTSerializable
import csw.messages.location.{ComponentType, Connection}
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

/**
 * The information needed to create a component
 * @param name                          The name of the component
 * @param componentType                 The type of the component as defined by [[csw.messages.location.ComponentType]]
 * @param prefix                        Identifies the subsystem
 * @param behaviorFactoryClassName      Specifies the component to be created by name of the class of it's factory
 * @param locationServiceUsage          Specifies component's usage of location service
 * @param connections                   Set of connections that will be used by this component for interaction
 * @param initializeTimeout             The timeout value used while initializing a component
 */
//TODO: add where it is used and how
final case class ComponentInfo(
    name: String,
    componentType: ComponentType,
    prefix: String,
    behaviorFactoryClassName: String,
    locationServiceUsage: LocationServiceUsage,
    connections: Set[Connection] = Set.empty,
    initializeTimeout: FiniteDuration = 10.seconds
) extends TMTSerializable {

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: java.util.List[Connection] = connections.toList.asJava
}

case object ComponentInfo {

  //TODO: why do we need below vals and how it is used
  implicit val finiteDurationReads: Reads[FiniteDuration]   = Reads[FiniteDuration](parseDuration)
  implicit val finiteDurationWrites: Writes[FiniteDuration] = Writes[FiniteDuration](d ⇒ Json.toJson(d.toString))
  implicit val componentInfoFormat: OFormat[ComponentInfo]  = Jsonx.formatCaseClassUseDefaults[ComponentInfo]

  //TODO: what is this method and how it is used
  private def parseDuration(json: JsValue): JsResult[FiniteDuration] = json.validate[String].flatMap { str =>
    str.split(" ") match {
      case Array(length: String, unit: String) => JsSuccess(FiniteDuration.apply(length.toLong, unit))
      case _                                   => JsError("error.expected.duration.finite")
    }
  }
}
