package csw.messages.ccs.commands

import java.util.Optional

import csw.messages.params.models.{ObsId, Prefix}

import scala.compat.java8.OptionConverters.RichOptionForJava8

trait CommandInfoHelper {

  def commandInfo: CommandInfo

  def originationPrefix: Prefix    = commandInfo.originationPrefix
  def prefix: Prefix               = commandInfo.prefix
  def maybeObsId: Option[ObsId]    = commandInfo.maybeObsId
  def jMaybeObsId: Optional[ObsId] = commandInfo.maybeObsId.asJava
}
