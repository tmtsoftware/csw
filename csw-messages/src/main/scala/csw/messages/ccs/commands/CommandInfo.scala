package csw.messages.ccs.commands

import java.util.Optional

import csw.messages.TMTSerializable
import csw.messages.params.models.{ObsId, Prefix}
import play.api.libs.json.{Format, Json}

import scala.compat.java8.OptionConverters.{RichOptionForJava8, RichOptionalGeneric}

/**
 * This will include information related to the observation that is tied to a parameter set
 */
case class CommandInfo private (originationPrefix: Prefix, prefix: Prefix, maybeObsId: Option[ObsId]) extends TMTSerializable {
  def this(originationPrefix: String, prefix: String, maybeObsId: Optional[ObsId]) =
    this(originationPrefix, prefix, maybeObsId.asScala)
  def jMaybeObsId: Optional[ObsId] = maybeObsId.asJava
}

object CommandInfo {
  implicit val format: Format[CommandInfo] = Json.format[CommandInfo]
//  implicit def strToParamSetInfo(obsId: String): CommandInfo = CommandInfo(ObsId(obsId))
}
