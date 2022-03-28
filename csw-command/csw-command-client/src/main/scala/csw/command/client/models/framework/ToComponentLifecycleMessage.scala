/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.models.framework

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

/**
 * Represents protocol or messages sent to underlying TLA component
 */
sealed trait ToComponentLifecycleMessage extends EnumEntry

object ToComponentLifecycleMessage extends Enum[ToComponentLifecycleMessage] {

  override def values: immutable.IndexedSeq[ToComponentLifecycleMessage] = findValues

  /**
   * Represents an action to go to offline mode
   */
  case object GoOffline extends ToComponentLifecycleMessage

  /**
   * Represents an action to go to online mode
   */
  case object GoOnline extends ToComponentLifecycleMessage

  /**
   * A Java helper representing GoOffline
   */
  def jGoOffline(): ToComponentLifecycleMessage = GoOffline

  /**
   * A Java helper representing GoOnline
   */
  def jGoOnline(): ToComponentLifecycleMessage = GoOnline
}
