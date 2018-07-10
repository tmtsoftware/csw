package csw.messages.commands

import csw.messages.TMTSerializable

final class SubscriptionKey[T](val predicate: T ⇒ Boolean) extends TMTSerializable
object SubscriptionKey {
  def all[T] = new SubscriptionKey[T](_ ⇒ true)
}
