package csw.messages.framework

import ai.x.play.json.Jsonx
import csw.messages.TMTSerializable
import csw.messages.location.{ComponentType, Connection}
import csw.messages.params.models.Prefix
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

/**
 * The information needed to create a component. This class is created after de-serializing the config file for the component.
 *
 * @param name the name of the component
 * @param componentType
 *  : the type of the component as defined by [[csw.messages.location.ComponentType]]
 * @param prefix identifies the subsystem
 * @param behaviorFactoryClassName
 *  : specifies the component to be created by name of the class of it's factory
 * @param locationServiceUsage
 *  : specifies component's usage of location service
 * @param connections : set of connections that will be used by this component for interaction
 * @param initializeTimeout
 *  : the timeout value used while initializing a component
 */
final case class ComponentInfo(
    name: String,
    componentType: ComponentType,
    prefix: Prefix,
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

  // specifies how to serialize and de-serialize any FiniteDuration which is initializeTimeout in this case
  private[csw] implicit val finiteDurationReads: Reads[FiniteDuration] = Reads[FiniteDuration](parseDuration)
  private[csw] implicit val finiteDurationWrites: Writes[FiniteDuration] =
    Writes[FiniteDuration](d ⇒ Json.toJson(d.toString))

  private[csw] implicit val componentInfoFormat: OFormat[ComponentInfo] = Jsonx.formatCaseClassUseDefaults[ComponentInfo]

  private def parseDuration(json: JsValue): JsResult[FiniteDuration] = json.validate[String].flatMap { str =>
    str.split(" ") match {
      case Array(length: String, unit: String) => JsSuccess(FiniteDuration.apply(length.toLong, unit))
      case _                                   => JsError("error.expected.duration.finite")
    }
  }
}
