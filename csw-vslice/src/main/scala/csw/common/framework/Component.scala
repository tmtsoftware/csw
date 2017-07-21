package csw.common.framework

import csw.services.location.models.ComponentType.{Assembly, Container, HCD}
import csw.services.location.models.{ComponentType, Connection, ConnectionType}
import csw.common.framework.Component.ComponentInfo

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
 * Represents a Component, such as an assembly, HCD (Hardware Control Daemon) or SC (Sequence Component).
 */
object Component {

  /**
   * Describes how a component uses the location service
   */
  sealed trait LocationServiceUsage

  case object DoNotRegister extends LocationServiceUsage

  case object RegisterOnly extends LocationServiceUsage

  case object RegisterAndTrackServices extends LocationServiceUsage

  /**
   * The information needed to create a component
   */
  sealed trait ComponentInfo {

    /**
     * A unique name for the component
     */
    val componentName: String

    /**
     * The component type (HCD, Assembly, etc.)
     */
    val componentType: ComponentType

    /**
     * The name of the class that implements the component (used to create the class via reflection)
     */
    val componentClassName: String

    /**
     * Indicates if the component needs to be registered with the location service or lookup other services
     */
    val locationServiceUsage: LocationServiceUsage

    /**
     * A dot separated prefix (for example tcs.ao.mycomp) that applies to this component
     */
    val prefix: String
  }

  /**
   * Describes an HCD component
   *
   * @param componentName        name used to register the component with the location service
   * @param prefix               the configuration prefix (part of configs that component should receive)
   * @param componentClassName   The name of the class that implements the component (used to create the class via reflection)
   * @param locationServiceUsage how the component plans to use the location service
   * @param registerAs           register as an akka or http component or both
   * @param rate                 the HCD's refresh rate
   */
  final case class HcdInfo(
      componentName: String,
      prefix: String,
      componentClassName: String,
      locationServiceUsage: LocationServiceUsage,
      registerAs: Set[ConnectionType],
      rate: FiniteDuration
  ) extends ComponentInfo {
    val componentType = HCD
  }

  /**
   * Describes an Assembly component
   *
   * @param componentName        name used to register the component with the location service
   * @param prefix               the configuration prefix (part of configs that component should receive)
   * @param componentClassName   The name of the class that implements the component (used to create the class via reflection)
   * @param locationServiceUsage how the component plans to use the location service
   * @param registerAs           register as an akka or http component or both
   * @param connections          a list of connections that includes componentIds and connection Types
   */
  final case class AssemblyInfo(
      componentName: String,
      prefix: String,
      componentClassName: String,
      locationServiceUsage: LocationServiceUsage,
      registerAs: Set[ConnectionType],
      connections: Set[Connection]
  ) extends ComponentInfo {
    val componentType = Assembly

    /**
     * Java API to get the list of connections for the assembly
     */
    def getConnections: java.util.List[Connection] = connections.toList.asJava
  }

  /**
   * Describes a container component.
   *
   * @param componentName        name used to register the component with the location service
   * @param locationServiceUsage how the component plans to use the location service
   * @param registerAs           register as an akka or http component or both
   * @param componentInfos       information about the components contained in the container
   * @param initialDelay         only for testing
   * @param creationDelay        only for testing
   * @param lifecycleDelay       only for testing
   */
  final case class ContainerInfo(
      componentName: String,
      locationServiceUsage: LocationServiceUsage,
      registerAs: Set[ConnectionType],
      componentInfos: Set[ComponentInfo],
      initialDelay: FiniteDuration = 0.seconds,
      creationDelay: FiniteDuration = 0.seconds,
      lifecycleDelay: FiniteDuration = 0.seconds
  ) extends ComponentInfo {
    val componentType      = Container
    val componentClassName = "csw.services.pkg.ContainerComponent"
    val prefix             = ""
  }

//  private def createHCD(context: ActorContext, cinfo: ComponentInfo, supervisorIn: Option[ActorRef]): ActorRef = {
//
//    val supervisor = supervisorIn match {
//      case None                => context.self // Will be parent which is supervisor
//      case Some(supervisorRef) => supervisorRef
//    }
//
//    // Form props for component
//    val props = Props(Class.forName(cinfo.componentClassName), cinfo, supervisor)
//
//    context.actorOf(props, s"${cinfo.componentName}-${cinfo.componentType}")
//  }
//
//  private def createAssembly(context: ActorContext, cinfo: AssemblyInfo, supervisorIn: Option[ActorRef]): ActorRef = {
//
//    val supervisor = supervisorIn match {
//      case None                => context.self // Will be parent which is supervisor
//      case Some(supervisorRef) => supervisorRef
//    }
//
//    val props = Props(Class.forName(cinfo.componentClassName), cinfo, supervisor)
//
//    context.actorOf(props, s"${cinfo.componentName}-${cinfo.componentType}")
//  }
//
//  /**
//   * Creates a component from the given componentInfo
//   * @param context the actor context
//   * @param componentInfo describes the component
//   * @param supervisorIn optional supervisor actor to use instead of the default
//   * @return the component's supervisor
//   */
//  def create(context: ActorContext, componentInfo: ComponentInfo, supervisorIn: Option[ActorRef] = None): ActorRef =
//    componentInfo match {
//      case hcd: HcdInfo =>
//        createHCD(context, hcd, supervisorIn)
//      case ass: AssemblyInfo =>
//        createAssembly(context, ass, supervisorIn)
//      case cont: ContainerInfo =>
//        ContainerComponent.create(cont)
//    }
//
//  // This is for JComponent create
//  import scala.compat.java8.OptionConverters._
//
//  /**
//   * Java API to create a component from the given componentInfo
//   * @param context the actor context
//   * @param componentInfo describes the component
//   * @param supervisorIn optional supervisor actor to use instead of the default
//   * @return the component's supervisor
//   */
//  def create(context: ActorContext, componentInfo: ComponentInfo, supervisorIn: Optional[ActorRef]): ActorRef =
//    create(context, componentInfo, supervisorIn.asScala)
}

trait Component {
  def info: ComponentInfo
}

trait Assembly extends Component

trait Hcd extends Component

trait Container extends Component
