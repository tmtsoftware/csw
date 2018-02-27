package csw.messages.ccs.commands

/**
 * Contains a list of commands that can be sent to a sequencer
 */
final case class CommandList(commands: Seq[SequenceCommand])
