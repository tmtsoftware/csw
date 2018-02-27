package csw.messages.params.states

import scala.collection.JavaConverters.seqAsJavaListConverter

/**
 * Combines multiple CurrentState objects together
 *
 * @param states one or more CurrentStates
 */
//TODO: explain better significance
final case class CurrentStates(states: Seq[CurrentState]) {

  /**
   * Java API: Returns the list of CurrentState objects
   */
  def jStates: java.util.List[CurrentState] = states.asJava
}
