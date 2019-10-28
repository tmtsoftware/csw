package csw.params.commands

import csw.params.core.models.Id
import csw.serializable.CommandSerializable

import scala.jdk.CollectionConverters._

final case class Sequence private[params] (runId: Id, commands: Seq[SequenceCommand]) extends CommandSerializable {
  def add(others: SequenceCommand*): Sequence = copy(commands = commands ++ others)
  def add(other: Sequence): Sequence          = copy(commands = commands ++ other.commands)
}

object Sequence {
  def apply(command: SequenceCommand, commands: SequenceCommand*): Sequence = Sequence(Id(), command :: commands.toList)

  def create(commands: java.util.List[SequenceCommand]): Sequence = Sequence(Id(), commands.asScala.toList)
}
