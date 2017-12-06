package csw.messages.ccs.commands

import csw.messages.params.generics.{Key, KeyType}

object Keys {
  val CancelKey: Key[String] = KeyType.StringKey.make("cancelKey")
}
