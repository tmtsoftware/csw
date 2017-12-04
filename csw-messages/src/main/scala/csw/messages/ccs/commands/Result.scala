package csw.messages.ccs.commands

import java.util.Optional

import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix, RunId}

import scala.compat.java8.OptionConverters.RichOptionForJava8

/**
 * A parameters set for returning results
 *
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Result private (
    runId: RunId,
    prefix: Prefix,
    maybeObsId: Option[ObsId],
    paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
) extends ParameterSetType[Result]
    with ParameterSetKeyData {

  override protected def create(data: Set[Parameter[_]]) = new Result(runId, prefix, maybeObsId, data)

  // This is here for Java to construct with String
  def this(runId: RunId, prefix: String, obsId: ObsId) = this(runId, new Prefix(prefix), Some(obsId))
  def this(runId: RunId, prefix: String) = this(runId, new Prefix(prefix), None)

  def jMaybeObsId: Optional[ObsId] = maybeObsId.asJava
}

object Result {
  def apply(
      runId: RunId,
      prefix: Prefix,
      maybeObsId: Option[ObsId],
      paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
  ): Result =
    new Result(runId, prefix, maybeObsId).madd(paramSet)
}
