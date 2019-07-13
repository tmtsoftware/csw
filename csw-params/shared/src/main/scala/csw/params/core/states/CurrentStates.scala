package csw.params.core.states

import scala.jdk.CollectionConverters._

/**
 * Combines multiple CurrentState objects together
 *
 * @param states one or more CurrentStates
 */
final case class CurrentStates(states: Seq[CurrentState]) {

  /**
   * A Java helper that returns the list of CurrentState objects
   */
  def jStates: java.util.List[CurrentState] = states.asJava
}
