package csw.messages.ccs.commands

import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.StringKey

object LockToken {
  val Key: Key[String] = StringKey.make("componentUUID")
}
