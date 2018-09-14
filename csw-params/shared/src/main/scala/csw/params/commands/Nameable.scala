package csw.params.commands
import csw.params.core.states.StateName

trait Nameable[T] {
  def name(state: T): StateName
}
