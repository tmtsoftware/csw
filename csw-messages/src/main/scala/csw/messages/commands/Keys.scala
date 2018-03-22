package csw.messages.commands

import csw.messages.params.generics.{Key, KeyType}

/**
 * A helper class providing predefined parameter Keys
 */
object Keys {

  /**
   * Represents a StringKey with `cancelKey` as key name
   */
  val CancelKey: Key[String] = KeyType.StringKey.make("cancelKey")
}
