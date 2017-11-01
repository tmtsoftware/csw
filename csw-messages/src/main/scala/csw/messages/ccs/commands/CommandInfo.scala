package csw.messages.ccs.commands

import ai.x.play.json.Jsonx
import csw.messages.params.models.{ObsId, RunId}
import play.api.libs.json.OFormat

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
  implicit val format: OFormat[CommandInfo]                  = Jsonx.formatCaseClassUseDefaults[CommandInfo]
  implicit def strToParamSetInfo(obsId: String): CommandInfo = CommandInfo(ObsId(obsId))
}
