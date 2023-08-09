/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.api.models

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
 * Represents a type of connection offered by the Component
 *
 * @param entryName A name of the connection type e.g. pekko, http or tcp
 */
sealed abstract class ConnectionType private[location] (override val entryName: String) extends EnumEntry {

  /**
   * The name of the connection type
   */
  def name: String = entryName
}

object ConnectionType extends Enum[ConnectionType] {

  /**
   * Returns a sequence of all connection types
   */
  override def values: IndexedSeq[ConnectionType] = findValues

  /**
   * Represents a HTTP type of connection
   */
  case object HttpType extends ConnectionType("http")

  /**
   * Represents a TCP type of connection
   */
  case object TcpType extends ConnectionType("tcp")

  /**
   * Represents an Pekko type of connection
   */
  case object PekkoType extends ConnectionType("pekko")

}
