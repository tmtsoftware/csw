package csw.messages.params.states

import csw.messages.commands.Setup
import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.Prefix
import csw.messages.params.states.StateVariable.StateVariable

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
     * an optional initial set of items (keys with values)
     */
    val paramSet: Set[Parameter[_]]

    /**
     * identifies the name of the state
     */
    val stateName: StateName
  }

  /**
   * Type of a function that returns true if two state variables (demand and current)
   * match (or are close enough, which is implementation dependent)
   */
  type Matcher = (DemandState, CurrentState) => Boolean

  /**
   * The default matcher for state variables tests for an exact match
   *
   * @param demand the demand state
   * @param current the current state
   * @return true if the demand and current states match (in this case, are equal)
   */
  def defaultMatcher(demand: DemandState, current: CurrentState): Boolean =
    demand.stateName == current.stateName && demand.prefixStr == current.prefixStr && demand.paramSet == current.paramSet

  /**
   * A Java helper method to create CurrentState
   *
   * @param states one or more CurrentState objects
   * @return a new CurrentStates object containing all the given CurrentState objects
   */
  @varargs
  def createCurrentStates(states: CurrentState*): CurrentStates = CurrentStates(states)

  /**
   * A Java helper method to create CurrentState
   *
   * @param states one or more CurrentState objects
   * @return a new CurrentStates object containing all the given CurrentState objects
   */
  def createCurrentStates(states: java.util.List[CurrentState]): CurrentStates = CurrentStates(states.asScala)
}

/**
 * A state variable that indicates the ''demand'' or requested state.
 *
 * @param prefix identifies the target subsystem
 * @param stateName identifies the name of the state
 * @param paramSet an optional initial set of items (keys with values)
 */
case class DemandState private (prefix: Prefix, stateName: StateName, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]])
    extends ParameterSetType[DemandState]
    with ParameterSetKeyData
    with StateVariable {

  /**
   * Create a new DemandState instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of DemandState with provided data
   */
  override protected def create(data: Set[Parameter[_]]): DemandState = copy(paramSet = data)

  /**
   * A Java helper method to construct with String
   */
  def this(prefix: String, stateName: StateName) = this(Prefix(prefix), stateName)

  /**
   * A Java helper method to create a DemandState from a Setup
   */
  def this(stateName: StateName, command: Setup) = this(command.source, stateName, command.paramSet)
}

object DemandState {

  /**
   * A helper method to create DemandState
   *
   * @param prefix identifies the target subsystem
   * @param stateName identifies the name of the state
   * @param paramSet an optional initial set of items (keys with values)
   * @return an instance of DemandState
   */
  def apply(prefix: Prefix, stateName: StateName, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): DemandState =
    new DemandState(prefix, stateName).madd(paramSet)
}

/**
 * A state variable that indicates the ''current'' or actual state.
 *
 * @param prefix       identifies the target subsystem
 * @param stateName identifies the name of the state
 * @param paramSet     an optional initial set of items (keys with values)
 */
case class CurrentState private (
    prefix: Prefix,
    stateName: StateName,
    paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
) extends ParameterSetType[CurrentState]
    with ParameterSetKeyData
    with StateVariable {

  /**
   * Create a new CurrentState instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of CurrentState with provided data
   */
  override protected def create(data: Set[Parameter[_]]): CurrentState = copy(paramSet = data)

  /**
   * A Java helper method to construct with String
   */
  def this(prefix: String, currentStateName: StateName) = this(Prefix(prefix), currentStateName)

  /**
   * A Java helper method to create a CurrentState from a Setup
   */
  def this(currentStateName: StateName, command: Setup) = this(command.source, currentStateName, command.paramSet)
}

object CurrentState {

  /**
   * A helper method to create CurrentState
   *
   * @param prefix identifies the target subsystem
   * @param stateName identifies the name of the state
   * @param paramSet an optional initial set of items (keys with values)
   * @return an instance of CurrentState
   */
  def apply(
      prefix: Prefix,
      stateName: StateName,
      paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
  ): CurrentState = new CurrentState(prefix, stateName).madd(paramSet)
}
