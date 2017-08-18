package csw.common.framework.models

import java.util.Optional

import csw.services.location.models.ComponentType.{Assembly, Container}
import csw.services.location.models.{ComponentType, Connection}

import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters.RichOptionForJava8

/**
 * Describes how a component uses the location service
 */
sealed trait LocationServiceUsage

object LocationServiceUsages {
  case object DoNotRegister            extends LocationServiceUsage
  case object RegisterOnly             extends LocationServiceUsage
  case object RegisterAndTrackServices extends LocationServiceUsage

  val JDoNotRegister: LocationServiceUsage            = DoNotRegister
  val JRegisterOnly: LocationServiceUsage             = RegisterOnly
  val JRegisterAndTrackServices: LocationServiceUsage = RegisterAndTrackServices
}

/**
 * The information needed to create a component
 */
case class ComponentInfo(componentName: String,
                         componentType: ComponentType,
                         prefix: String,
                         componentClassName: String,
                         locationServiceUsage: LocationServiceUsage,
                         maybeConnections: Option[Set[Connection]] = None,
                         maybeComponentInfoes: Option[Set[ComponentInfo]] = None) {

  require(!componentName.isEmpty)
  require(if (Assembly == componentType) maybeConnections.isDefined else true)
  require(if (Container == componentType) maybeComponentInfoes.isDefined else true)

  /**
   * Java API to get the list of connections for the assembly
   */
  def getConnections: Optional[java.util.List[Connection]] = maybeConnections.map(_.toList.asJava).asJava
}

trait Component {
  def info: ComponentInfo
}

trait Assembly extends Component

trait Hcd extends Component

trait Container extends Component
