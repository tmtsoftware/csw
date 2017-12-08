package csw.messages.ccs.commands

import java.util.Optional

import csw.messages.params.generics.{Parameter, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix, RunId}

import scala.compat.java8.OptionConverters.{RichOptionForJava8, RichOptionalGeneric}

/**
 * Common trait for Setup, Observe and Wait commands
 */
sealed trait Command { self: ParameterSetType[_] ⇒

  def paramType: ParameterSetType[_] = self

  /**
   * A name identifying the type of parameter set, such as "setup", "observe".
   * This is used in the JSON and toString output.
   */
  def typeName: String

  /**
   * unique ID for command parameter set
   */
  val runId: RunId

  /**
   * an optional initial set of parameters (keys with values)
   */
  val paramSet: Set[Parameter[_]]

  val originationPrefix: Prefix
  val prefix: Prefix
  val maybeObsId: Option[ObsId]

  def jMaybeObsId: Optional[ObsId] = maybeObsId.asJava
}

/**
 * Trait for sequence parameter sets
 */
sealed trait SequenceCommand extends Command { self: ParameterSetType[_] ⇒
}

/**
 * Marker trait for control parameter sets
 */
sealed trait ControlCommand extends Command { self: ParameterSetType[_] ⇒
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Setup private (
    runId: RunId,
    originationPrefix: Prefix,
    prefix: Prefix,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]] = Set.empty
) extends ParameterSetType[Setup]
    with SequenceCommand
    with ControlCommand {
  override protected def create(data: Set[Parameter[_]]): Setup = copy(paramSet = data)
  def this(originationPrefix: String, prefix: String, maybeObsId: Optional[ObsId]) =
    this(RunId(), originationPrefix, prefix, maybeObsId.asScala) // Provided for Java construction
}

object Setup {

  // The default apply method is used only internally while reading the incoming json and de-serializing it to setup model
  private[messages] def apply(
      runId: RunId,
      originationPrefix: Prefix,
      prefix: Prefix,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ): Setup =
    new Setup(runId, originationPrefix, prefix, maybeObsId, paramSet) //madd is not required as this version of apply is only used for reading json

  // The apply method is used to create Setup command by end-user. RunId is not accepted and will be created internally to guarantee unique value.
  def apply(
      originationPrefix: Prefix,
      prefix: Prefix,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]] = Set.empty
  ): Setup =
    new Setup(RunId(), originationPrefix, prefix, maybeObsId).madd(paramSet) //madd ensures check for duplicate key
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Observe private (
    runId: RunId,
    originationPrefix: Prefix,
    prefix: Prefix,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]] = Set.empty
) extends ParameterSetType[Observe]
    with SequenceCommand
    with ControlCommand {

  override protected def create(data: Set[Parameter[_]]): Observe = copy(paramSet = data)
  def this(originationPrefix: String, prefix: String, maybeObsId: Optional[ObsId]) =
    this(RunId(), originationPrefix, prefix, maybeObsId.asScala) // Provided for Java construction
}

object Observe {
  private[messages] def apply(
      runId: RunId,
      originationPrefix: Prefix,
      prefix: Prefix,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ) =
    new Observe(runId, originationPrefix, prefix, maybeObsId, paramSet)

  def apply(
      originationPrefix: Prefix,
      prefix: Prefix,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]] = Set.empty
  ): Observe =
    new Observe(RunId(), originationPrefix, prefix, maybeObsId).madd(paramSet)
}

/**
 * A parameter set for setting telescope and instrument parameters. Constructor is private to ensure RunId is created internally to guarantee unique value.
 *
 * @param runId unique ID for this parameter set
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Wait private (
    runId: RunId,
    originationPrefix: Prefix,
    prefix: Prefix,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]] = Set.empty
) extends ParameterSetType[Wait]
    with SequenceCommand {

  override protected def create(data: Set[Parameter[_]]): Wait = copy(paramSet = data)
  def this(originationPrefix: String, prefix: String, maybeObsId: Optional[ObsId]) =
    this(RunId(), originationPrefix, prefix, maybeObsId.asScala) // Provided for Java construction
}

object Wait {

  private[messages] def apply(
      runId: RunId,
      originationPrefix: Prefix,
      prefix: Prefix,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]]
  ) =
    new Wait(runId, originationPrefix, prefix, maybeObsId, paramSet)

  def apply(originationPrefix: Prefix, prefix: Prefix, maybeObsId: Option[ObsId], paramSet: Set[Parameter[_]] = Set.empty): Wait =
    new Wait(RunId(), originationPrefix, prefix, maybeObsId).madd(paramSet)
}
