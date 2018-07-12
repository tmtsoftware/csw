package csw.messages.commands

trait Nameable[T] {
  def name(state: T): String
}
