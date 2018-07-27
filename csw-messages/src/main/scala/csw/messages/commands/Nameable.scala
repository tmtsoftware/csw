package csw.messages.commands

import csw.messages.framework.LifecycleStateChanged
import csw.messages.params.states.{CurrentState, StateName}

/**
  * The Nameable typeclass provides a name function that returns a String that can be used for comparing
  * within the [[csw.framework.internal.pubsub]] actor to allow the subscribeOnly by Nameable
  *
  * @tparam T - Type supporting the Nameable typeclass
  */
trait Nameable[T] {
  def name(state: T): String
}

object Nameable {

  /**
    * This allows use of Nameable[A}
    */
  def apply[A](implicit nm: Nameable[A]):Nameable[A] = nm

  def name[A: Nameable](thing: A): String = Nameable[A].name(thing)

  implicit class NameableOps[A](val thing: A) extends AnyVal {
    def name(implicit nm: Nameable[A]): String = nm.name(thing)
  }

  /**
    * The following three implicit objects are required to support the Nameable typeclass used
    * by [[csw.framework.internal.pubsub]] to allow subscribeOnly
    * These evidence classes are here so Nameable is easier to understand and it isn't scattered across the source tree
    */
  implicit object NameableLifecycleStateChanged extends Nameable[LifecycleStateChanged] {
    override def name(state: LifecycleStateChanged): String = state.state.toString
  }

  implicit object NameableCurrentState extends Nameable[CurrentState] {
    override def name(state: CurrentState): String = state.stateName.name
  }

  implicit object StateNameIsNameable extends Nameable[StateName] {
    def name(sn: StateName): String = sn.name
  }

}
