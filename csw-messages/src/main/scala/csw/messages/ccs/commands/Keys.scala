package csw.messages.ccs.commands

import csw.messages.params.generics.{Key, KeyType}

object Keys {
  val runIdToCancelKey: Key[String] = KeyType.StringKey.make("runIdToCancel")
}
