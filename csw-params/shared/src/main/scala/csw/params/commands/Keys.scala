/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.commands

import csw.params.core.generics.{Key, KeyType}

/**
 * A helper class providing predefined parameter Keys
 */
object Keys {

  /**
   * Represents a StringKey with `cancelKey` as key name
   */
  val CancelKey: Key[String] = KeyType.StringKey.make("cancelKey")
}
