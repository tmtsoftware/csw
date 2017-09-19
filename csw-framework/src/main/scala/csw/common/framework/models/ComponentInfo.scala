package csw.common.framework.models

import ai.x.play.json.Jsonx
import csw.services.location.models.{ComponentType, Connection}
import play.api.libs.json._

import scala.collection.JavaConverters._

/**
 * The information needed to create a component
 */
final case class ComponentInfo(
    name: String,
    componentType: ComponentType,
    prefix: String,
    behaviorFactoryClassName: String,
    locationServiceUsage: LocationServiceUsage,
    connections: Set[Connection] = Set.empty,
    initializeTimeoutInSeconds: Int = 10,
    runTimeoutInSeconds: Int = 10
) {

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: java.util.List[Connection] = connections.toList.asJava
}

case object ComponentInfo {
  implicit val componentInfoFormat: OFormat[ComponentInfo] = Jsonx.formatCaseClassUseDefaults[ComponentInfo]
}
