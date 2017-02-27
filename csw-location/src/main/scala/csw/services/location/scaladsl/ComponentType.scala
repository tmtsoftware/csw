package csw.services.location.scaladsl

import scala.util.{Failure, Success, Try}

/**
 * CSW Component types
 */
sealed trait ComponentType

object ComponentType {

  /**
   * A container for components (assemblies and HCDs)
   */
  case object Container extends ComponentType

  /**
   * A component that controls a hardware device
   */
  case object HCD extends ComponentType

  /**
   * A component that controls one or more HCDs or assemblies
   */
  case object Assembly extends ComponentType

  /**
   * A general purpose service component (actor and/or web service application)
   */
  case object Service extends ComponentType
  /**
   * Exception thrown when a string can not be parsed to a component type
   */
  case class UnknownComponentTypeException(message: String) extends Exception(message)

  /**
   * Returns the named component type or an UnknownComponentTypeException exception if not known
   */
  def apply(name: String): Try[ComponentType] = name.toLowerCase match {
    case "container" => Success(Container)
    case "assembly"  => Success(Assembly)
    case "hcd"       => Success(HCD)
    case "service"   => Success(Service)
    case x           => Failure(UnknownComponentTypeException(x))
  }
}
