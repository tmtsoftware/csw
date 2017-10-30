package csw.messages.ccs.commands

import csw.messages.params.generics.{Parameter, ParameterSetKeyData, ParameterSetType}
import csw.messages.params.models.{ObsId, Prefix, RunId}

/**
 * A parameters set for returning results
 *
 * @param prefix   identifies the target subsystem
 * @param paramSet an optional initial set of parameters (keys with values)
 */
case class Result private (
    runId: RunId,
    obsId: ObsId,
    prefix: Prefix,
    paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]
) extends ParameterSetType[Result]
    with ParameterSetKeyData {

  override protected def create(data: Set[Parameter[_]]) = new Result(runId, obsId, prefix, data)

  // This is here for Java to construct with String
  def this(runId: RunId, obsId: ObsId, prefix: String) = this(runId, obsId, new Prefix(prefix))
}

object Result {
  def apply(runId: RunId, obsId: ObsId, prefix: Prefix, paramSet: Set[Parameter[_]] = Set.empty[Parameter[_]]): Result =
    new Result(runId, obsId, prefix).madd(paramSet)
}
