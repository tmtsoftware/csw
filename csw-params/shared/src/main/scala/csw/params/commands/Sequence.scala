package csw.params.commands

import csw.serializable.CommandSerializable

final case class Sequence private[params] (commands: Seq[SequenceCommand]) extends CommandSerializable {
  def add(others: SequenceCommand*): Sequence = copy(commands = commands ++ others)
  def add(other: Sequence): Sequence = copy(commands = commands ++ other.commands)
}

object Sequence {
  def apply(command: SequenceCommand, commands: SequenceCommand*): Sequence = Sequence(command :: commands.toList)
}
