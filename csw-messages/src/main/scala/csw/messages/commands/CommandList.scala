package csw.messages.commands

/**
 * Contains a list of commands that can be sent to a sequencer
 */
final case class CommandList(paramSets: Seq[SequenceCommand])
