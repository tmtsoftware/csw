package csw.messages.commands

/**
 * Contains a list of commands that can be sent to a sequencer
 *
 * @param commands sequence of SequenceCommand
 */
final case class CommandList(commands: Seq[SequenceCommand])
