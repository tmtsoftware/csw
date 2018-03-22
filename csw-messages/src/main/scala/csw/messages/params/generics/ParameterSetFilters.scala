package csw.messages.params.generics

import csw.messages.commands.{Observe, SequenceCommand, Setup, Wait}
import csw.messages.params.models.Subsystem

/**
 * A collection of Utility functions for filtering Commands and Parameters from an input sequence
 */
object ParameterSetFilters {

  /**
   * A filter type for various parameter set data
   *
   */
  type ParamSetFilter[A] = A => Boolean

  private val prefixStartsWithFilter: String => ParamSetFilter[ParameterSetKeyData] = query =>
    sc => sc.prefixStr.startsWith(query)
  private val prefixContainsFilter: String => ParamSetFilter[ParameterSetKeyData] = query => sc => sc.prefixStr.contains(query)
  private val prefixIsSubsystem: Subsystem => ParamSetFilter[ParameterSetKeyData] = query => sc => sc.subsystem.equals(query)

  /**
   * Gives only prefixes from given Seq of ParameterSetKeyData
   *
   * @param paramSets a Seq of ParameterSetKeyData
   * @return a Set of prefixes
   */
  def prefixes(paramSets: Seq[ParameterSetKeyData]): Set[String] = paramSets.map(_.prefixStr).toSet

  /**
   * Gives only Setup type of commands from given Seq of SequenceCommand
   *
   * @param sequenceCommands a Seq of SequenceCommand
   * @return a Seq of Setup commands
   */
  def onlySetups(sequenceCommands: Seq[SequenceCommand]): Seq[Setup] = sequenceCommands.collect { case ct: Setup => ct }

  /**
   * Gives only Observe type of commands from given Seq of SequenceCommand
   *
   * @param sequenceCommands a Seq of SequenceCommand
   * @return a Seq of Observe commands
   */
  def onlyObserves(sequenceCommands: Seq[SequenceCommand]): Seq[Observe] = sequenceCommands.collect { case ct: Observe => ct }

  /**
   * Gives only Wait type of commands from given Seq of SequenceCommand
   *
   * @param sequenceCommands a Seq of SequenceCommand
   * @return a Seq of Wait commands
   */
  def onlyWaits(sequenceCommands: Seq[SequenceCommand]): Seq[Wait] = sequenceCommands.collect { case ct: Wait => ct }

  /**
   * Gives a Seq of ParameterSetKeyData for which the prefix starts with given query
   *
   * @param query a String to which the prefix is matched at the beginning
   * @param paramSets a Seq of ParameterSetKeyData
   * @return a filtered Seq of ParameterSetKeyData
   */
  def prefixStartsWith(query: String, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
    paramSets.filter(prefixStartsWithFilter(query))

  /**
   * Gives a Seq of ParameterSetKeyData for which the prefix contains with given query
   *
   * @param query a String to which the prefix is matched anywhere in the word
   * @param paramSets a Seq of ParameterSetKeyData
   * @return a filtered Seq of ParameterSetKeyData
   */
  def prefixContains(query: String, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
    paramSets.filter(prefixContainsFilter(query))

  /**
   * Gives a Seq of ParameterSetKeyData for which the prefix contains with given query
   *
   * @param query a String to which the prefix is matched exactly with
   * @param paramSets a Seq of ParameterSetKeyData
   * @return a filtered Seq of ParameterSetKeyData
   */
  def prefixIsSubsystem(query: Subsystem, paramSets: Seq[ParameterSetKeyData]): Seq[ParameterSetKeyData] =
    paramSets.filter(prefixIsSubsystem(query))
}
