package csw.param

import csw.param.Parameters._
import csw.param.parameters.Parameter

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scala.language.implicitConversions

object StateVariable {

  /**
   * Base trait for state variables
   */
  sealed trait StateVariable extends Serializable {

    /**
     * A name identifying the type of command, such as "setup", "observe".
     * This is used in the JSON and toString output.
     */
    def typeName: String

    /**
     * identifies the target subsystem
     */
    val prefix: Prefix

    /**
     * an optional initial gset of items (keys with values)
     */
    val paramSet: ParameterSet
  }

  /**
   * Type of a function that returns true if two state variables (demand and current)
   * match (or are close enough, which is implementation dependent)
   */
  type Matcher = (DemandState, CurrentState) => Boolean

  /**
   * The default matcher for state variables tests for an exact match
   *
   * @param demand  the demand state
   * @param current the current state
   * @return true if the demand and current states match (in this case, are equal)
   */
  def defaultMatcher(demand: DemandState, current: CurrentState): Boolean =
    demand.prefixStr == current.prefixStr && demand.paramSet == current.paramSet

  /**
   * A state variable that indicates the ''demand'' or requested state.
   *
   * @param prefix identifies the target subsystem
   * @param paramSet     an optional initial gset of items (keys with values)
   */
  case class DemandState(prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[DemandState]
      with ParameterSetKeyData
      with StateVariable {

    override def create(data: ParameterSet) = DemandState(prefix, data)

    /**
     * This is here for Java to construct with String
     */
    def this(prefix: String) = this(Prefix.stringToPrefix(prefix))

    /**
     * Java API to create a DemandState from a Setup
     */
    def this(command: Setup) = this(command.prefixStr, command.paramSet)
  }

  object DemandState {

    /**
     * Converts a Setup to a DemandState
     */
    implicit def apply(command: Setup): DemandState = DemandState(command.prefixStr, command.paramSet)
  }

  /**
   * A state variable that indicates the ''current'' or actual state.
   *
   * @param prefix identifies the target subsystem
   * @param paramSet     an optional initial gset of items (keys with values)
   */
  case class CurrentState(prefix: Prefix, paramSet: ParameterSet = Set.empty[Parameter[_]])
      extends ParameterSetType[CurrentState]
      with ParameterSetKeyData
      with StateVariable {

    override def create(data: ParameterSet) = CurrentState(prefix, data)

    /**
     * This is here for Java to construct with String
     */
    def this(prefix: String) = this(Prefix.stringToPrefix(prefix))

    /**
     * Java API to create a DemandState from a Setup
     */
    def this(command: Setup) = this(command.prefixStr, command.paramSet)

  }

  object eCurrentState {

    /**
     * Converts a Setup to a CurrentState
     */
    implicit def apply(command: Setup): CurrentState = CurrentState(command.prefixStr, command.paramSet)

    /**
     * Java API to create a CurrentState from a Setup
     */
    def fromSetupCommand(command: Setup): CurrentState = CurrentState(command.prefixStr, command.paramSet)
  }

  /**
   * Combines multiple CurrentState objects together
   *
   * @param states one or more CurrentStates
   */
  final case class CurrentStates(states: Seq[CurrentState]) {

    /**
     * Java API: Returns the list of CurrentState objects
     */
    def jstates: java.util.List[CurrentState] = states.asJava
  }

  /**
   * For the Java API
   *
   * @param states one or more CurrentState objects
   * @return a new CurrentStates object containing all the given CurrentState objects
   */
  @varargs
  def createCurrentStates(states: CurrentState*): CurrentStates = CurrentStates(states)

  /**
   * For the Java API
   *
   * @param states one or more CurrentState objects
   * @return a new CurrentStates object containing all the given CurrentState objects
   */
  def createCurrentStates(states: java.util.List[CurrentState]): CurrentStates = CurrentStates(states.asScala)
}
