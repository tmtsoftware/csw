/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.commands

import csw.serializable.CommandSerializable

import scala.jdk.CollectionConverters.*

/**
 * a sequence of [[csw.params.commands.SequenceCommand]]
 */
final case class Sequence private[params] (commands: Seq[SequenceCommand]) extends CommandSerializable {
  def add(others: SequenceCommand*): Sequence = copy(commands = commands ++ others)
  def add(other: Sequence): Sequence          = copy(commands = commands ++ other.commands)
}

object Sequence {
  def apply(command: SequenceCommand, commands: SequenceCommand*): Sequence = Sequence(command :: commands.toList)

  /**
   * Create a Sequence  model from a  list of [[csw.params.commands.SequenceCommand]]
   */
  def create(commands: java.util.List[SequenceCommand]): Sequence = Sequence(commands.asScala.toList)
}
