package csw.messages.commands
import csw.messages.params.states.StateName

trait Nameable[T] {
  def name(state: T): StateName
}
