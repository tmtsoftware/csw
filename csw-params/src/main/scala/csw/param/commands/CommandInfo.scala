package csw.param.commands

import csw.param.models.params.{ObsId, RunId}
import spray.json.JsonFormat

import scala.language.implicitConversions

/**
 * This will include information related to the observation that is tied to a parameter set
 * This will grow and develop.
 *
 * @param obsId the observation id
 * @param runId unique ID for this parameter set
 */
case class CommandInfo(obsId: ObsId, runId: RunId = RunId()) {

  /**
   * Creates an instance with the given obsId and a unique runId
   */
  def this(obsId: String) = this(ObsId(obsId))
}

object CommandInfo {
  import spray.json.DefaultJsonProtocol._
  implicit val format: JsonFormat[CommandInfo]               = jsonFormat2(CommandInfo.apply)
  implicit def strToParamSetInfo(obsId: String): CommandInfo = CommandInfo(ObsId(obsId))
}
