package csw.param

import csw.param.Events.{EventInfo, ObserveEvent, StatusEvent, SystemEvent}
import csw.param.Parameters._
import csw.param.UnitsOfMeasure.{NoUnits, Units}
import csw.param.parameters.{Key, Parameter}

/**
 * Defines a Scala DSL for dealing with configurations.
 * See ConfigDSLTests for example usage.
 */
object ParameterSetDsl {

  /**
   * Returns the number of values in the parameter
   *
   * @param parameter Some parameter instance
   * @return the number of values as an Int
   */
  def size[P <: Parameter[_]](parameter: P): Int = parameter.size

  /**
   * Returns the units of a parameter
   *
   * @param parameter Some parameter instance
   * @return the units associated with the data in this parameter
   */
  def units[P <: Parameter[_]](parameter: P): Units = parameter.units

  /**
   * Add a parameter to the configuration T
   *
   * @param sc   the configuration to contain the parameters
   * @param parameter the parameter to add
   * @return a new configuration with the parameter added or updating previously existing parameter
   */
  def add[P <: Parameter[_], T <: ParameterSetType[T]](sc: T, parameter: P): T = sc.add(parameter)

  /**
   * Add one or more parameters to the configuration
   *
   * @param sc    the configuration to contain the parameters
   * @param parameters the parameters to add
   * @return a new configuration with the parameters added or updated previously existing parameter
   */
  def madd[P <: Parameter[_], T <: ParameterSetType[T]](sc: T, parameters: P*): T = sc.madd(parameters: _*)

  /**
   * Check to see if a parameter exists using a key
   *
   * @param sc  The configuration that contains parameters
   * @param key the key of the parameter to be tested for existence
   * @return true if the parameter is present and false if not present
   */
  def exists[S, P <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, P]): Boolean = sc.exists(key)

  /**
   * Return the number of parameters in the configuration
   *
   * @param sc The configuration that contains parameters
   * @return the number of parameters in the configuration
   */
  def csize[T <: ParameterSetType[_]](sc: T): Int = sc.size

  /**
   * Remove a parameter from the configuration based on key
   *
   * @param sc  the configuration that contains parameters
   * @param key the key of the parameter to remove
   * @return a new configuration with the parameter with key removed or unchanged if not present
   */
  def remove[S, P <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, P]): T = sc.remove(key)

  /**
   * Remove a parameter from the configuration based on the parameter contents
   *
   * @param sc   the configuration that contains parameters
   * @param parameter the parameter to be removed
   * @return a new configuration with the parameter removed or unchanged if not present
   */
  def remove[P <: Parameter[_], T <: ParameterSetType[T]](sc: T, parameter: P): T = sc.remove(parameter)

  /**
   * Find the parameter in the configuration
   *
   * @param sc  the configuration that contains parameters
   * @param key the key of the parameter that is needed
   * @return returns the parameter itself or the NoSuchElementException if the key is not present
   */
  def parameter[S, P <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, P]): P = sc.parameter(key)

  /**
   * Find the parameter in the configuraiton and return as Option with the parameter
   *
   * @param sc  the configuration that contains parameters
   * @param key the key of the parameter that is needed
   * @return the parameter as an Option or None if the parameter is not found
   */
  def get[S, P <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, P]): Option[P] = sc.get(key)

  /**
   * Finds a parameter and returns the value at an index as an Option
   * This is a shortcut for get parameter and get(index) value
   *
   * @param sc    the configuration that contains parameters
   * @param key   the key of the parameter that is needed
   * @param index the index of the value needed
   * @return the index value as an Option or None if the parameter with key is not present or there is no value at the index
   */
  def get[S, P <: Parameter[S], T <: ParameterSetType[T]](sc: T, key: Key[S, P], index: Int): Option[S] =
    sc.get(key).flatMap((i: Parameter[S]) => i.get(index))

  /**
   * Convenience function to return the first value parameter
   *
   * @param parameter the parameter that contains values
   * @return The parameter at the front of the values
   */
  def head[S](parameter: Parameter[S]): S = parameter.head

  /**
   * Returns the value for a parameter at the index
   *
   * @param parameter  the parameter that contains values
   * @param index the index of the needed value
   * @return the parameter's index value or throws an IndexOutOfBoundsException
   */
  def value[S](parameter: Parameter[S], index: Int): S = {
    parameter.value(index)
  }

  /**
   * Returns the value for a parameter at the index as an Option
   *
   * @param parameter  the parameter that contains values
   * @param index the index of the needed value
   * @return the parameter's index value as an Option (i.e. Some(value)) or None if the index is inappropriate
   */
  def get[S](parameter: Parameter[S], index: Int): Option[S] = parameter.get(index)

  /**
   * Returns the vector of values for the parameter
   *
   * @param parameter the parameter with the needed values
   * @return all of the values for the parameter as a Vector
   */
  def values[S](parameter: Parameter[S]): Vector[S] = parameter.values

  /**
   * Create a parameter by setting a key with a Vector of values associated with the key
   *
   * @param key   the key that is used to create the needed parameter
   * @param v     a Vector of values of the parameter's type that is being used to set the parameter
   * @param units optional units for the parameter
   * @return a new parameter of the type associated with the key
   */
  def vset[S, P <: Parameter[S]](key: Key[S, P], v: Vector[S], units: Units = NoUnits): P = key.set(v, units)

  /**
   * Create a parameter by settign a key with one or more values associated with the key
   *
   * @param key the key that isused to crate the needed parameter
   * @param v   a varargs argument with one or more values of the parameter's type
   * @return a new parameter of the type associated with the key
   */
  def set[S, P <: Parameter[S]](key: Key[S, P], v: S*): P = key.set(v: _*)

  /**
   * Create a Setup with a number of parameters
   *
   * @param info      information related to the parameter set
   * @param prefix can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new Setup with the parameters added
   */
  def setup(info: CommandInfo, prefix: Prefix, parameters: Parameter[_]*): Setup =
    Setup(info, prefix).madd(parameters: _*)

  /**
   * Create an Observe with a number of parameters
   *
   * @param info information related to the parameter set
   * @param prefix can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new Observe with the parameters added
   */
  def observe(info: CommandInfo, prefix: Prefix, parameters: Parameter[_]*): Observe =
    Observe(info, prefix).madd(parameters: _*)

  /**
   * Create a CurrentState with a number of parameters
   *
   * @param prefix can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new CurrentState with the parameters added
   */
  def cs(prefix: Prefix, parameters: Parameter[_]*): StateVariable.CurrentState =
    StateVariable.CurrentState(prefix).madd(parameters: _*)

  /**
   * Create a DemandState with a number of parameters
   *
   * @param prefix can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new DemandState with the parameters added
   */
  def ds(prefix: Prefix, parameters: Parameter[_]*): StateVariable.DemandState =
    StateVariable.DemandState(prefix).madd(parameters: _*)

  /**
   * Create an ObserveEvent with a number of parameters
   *
   * @param eventInfo and EventInfo object, or can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new ObserveEvent with the parameters added
   */
  def oe(eventInfo: EventInfo, parameters: Parameter[_]*): ObserveEvent = ObserveEvent(eventInfo).madd(parameters: _*)

  /**
   * Create an StatusEvent with a number of parameters
   *
   * @param eventInfo and EventInfo object, or can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new StatusEvent with the parameters added
   */
  def stEv(eventInfo: EventInfo, parameters: Parameter[_]*): StatusEvent = StatusEvent(eventInfo).madd(parameters: _*)

  /**
   * Create an SystemEvent with a number of parameters
   *
   * @param eventInfo and EventInfo object, or can be a String form - "wfos.red.filter
   * @param parameters     0 or more parameters to be added during creation
   * @return a new SystemEvent with the parameters added
   */
  def sysEv(eventInfo: EventInfo, parameters: Parameter[_]*): SystemEvent = SystemEvent(eventInfo).madd(parameters: _*)
}
