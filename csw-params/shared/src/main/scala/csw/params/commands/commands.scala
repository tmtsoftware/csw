/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.params.commands

import java.util.Optional

import csw.params.core.generics.{Parameter, ParameterSetType}
import csw.params.core.models.ObsId
import csw.params.extensions.OptionConverters.{RichOption, RichOptional}
import csw.prefix.models.Prefix

/**
 * Common trait representing commands in TMT like Setup, Observe and Wait
 */
sealed trait Command extends ParameterSetType[Command] {

  /**
   * A helper to give access of public members of ParameterSetType
   *
   * @return a handle to ParameterSetType extended by concrete implementation of this class
   */
  def paramType: ParameterSetType[_] = this

  /**
   * A name identifying the type of parameter set, such as "setup", "observe".
   * This is used in toString output and while de-serializing from JSON.
   *
   * @return a string representation of concrete type of this class
   */
  def typeName: String

  /**
   * An optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  /**
   * Prefix representing source of the command
   */
  val source: Prefix

  /**
   * The name of command
   */
  val commandName: CommandName

  /**
   * An optional obsId for command
   */
  val maybeObsId: Option[ObsId]

  /**
   * A Java helper to acsess optional obsId
   *
   * @return an Optional of ObsId
   */
  def jMaybeObsId: Optional[ObsId] = maybeObsId.asJava

  /**
   * A common toString method for all concrete implementation
   *
   * @return the string representation of command
   */
  override def toString: String =
    s"${this.typeName}(paramSet=$paramSet, source=$source, commandName=$commandName, maybeObsId=$maybeObsId)"
}

/**
 * Marker trait for sequence parameter sets which is applicable to Sequencer type of components
 */
sealed trait SequenceCommand extends Command

/**
 * Marker trait for control parameter sets which i applicable to Assembly and HCD type of components
 */
sealed trait ControlCommand extends SequenceCommand

/**
 * A parameter set for setting telescope and instrument parameters.
 */
case class Setup private[params] (
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId] = None,
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Setup]
    with ControlCommand {

  /**
   * A java helper to construct Setup command
   */
  def this(source: Prefix, commandName: CommandName, maybeObsId: Optional[ObsId]) =
    this(source, commandName, maybeObsId.asScala, Set.empty)

  /**
   * Create a new Setup instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Setup with provided data
   */
  override protected def create(data: Set[Parameter[_]]): Setup = copy(paramSet = data)
}

object Setup {

  /**
   * The apply method is used to create Setup command by end-user.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @return a new instance of Setup with empty paramSet
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId] = None): Setup =
    new Setup(source, commandName, maybeObsId, Set.empty)

  /**
   * The apply method is used to create Setup command by end-user.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of Setup
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Setup =
    apply(source, commandName, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters.
 */
case class Observe private[params] (
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId] = None,
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Observe]
    with ControlCommand {

  /**
   * A java helper to construct Observe command
   */
  def this(source: Prefix, commandName: CommandName, maybeObsId: Optional[ObsId]) =
    this(source, commandName, maybeObsId.asScala, Set.empty)

  /**
   * Create a new Observe instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Observe with new provided data
   */
  override protected def create(data: Set[Parameter[_]]): Observe = copy(paramSet = data)
}

object Observe {

  /**
   * The apply method is used to create Observe command by end-user.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @return a new instance of Observe with empty paramSet
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId] = None): Observe =
    new Observe(source, commandName, maybeObsId, Set.empty)

  /**
   * The apply method is used to create Observe command by end-user.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of Observe
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Observe =
    apply(source, commandName, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters.
 */
case class Wait private[params] (
    source: Prefix,
    commandName: CommandName,
    maybeObsId: Option[ObsId] = None,
    paramSet: Set[Parameter[_]]
) extends ParameterSetType[Wait]
    with SequenceCommand {

  /**
   * A java helper to construct Wait command
   */
  def this(source: Prefix, commandName: CommandName, maybeObsId: Optional[ObsId]) =
    this(source, commandName, maybeObsId.asScala, Set.empty)

  /**
   * Create a new Wait instance when a parameter is added or removed
   *
   * @param data set of parameters
   * @return a new instance of Wait with new provided data
   */
  override protected def create(data: Set[Parameter[_]]): Wait = copy(paramSet = data)
}

object Wait {

  /**
   * The apply method is used to create Wait command by end-user.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @return a new instance of Wait with empty paramSet
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId] = None): Wait =
    apply(source, commandName, maybeObsId, Set.empty)

  /**
   * The apply method is used to create Wait command by end-user.
   *
   * @param source prefix representing source of the command
   * @param commandName the name of the command
   * @param maybeObsId an optional obsId for command
   * @param paramSet an initial set of parameters (keys with values)
   * @return a new instance of Wait
   */
  def apply(source: Prefix, commandName: CommandName, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]]): Wait =
    new Wait(source, commandName, maybeObsId, Set.empty).madd(paramSet)
}
