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

/**
 * Represents a Component, such as an assembly, HCD (Hardware Control Daemon) or SC (Sequence Component).
 */
object ComponentInfo {

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
