package csw.messages.ccs.commands

import csw.messages.params.generics.{Key, KeyType}

//TODO: add doc what and why and how
object Keys {
  val CancelKey: Key[String] = KeyType.StringKey.make("cancelKey")
}
