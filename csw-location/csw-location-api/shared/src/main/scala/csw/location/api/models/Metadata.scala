package csw.location.api.models

import java.util
import java.util.Optional

import csw.location.api.models.Metadata.{AgentPrefixKey, PidKey}
import csw.prefix.models.Prefix

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOption

/**
 * metadata represents any additional information (metadata) associated with location
 * For example, "agentId": "ESW.agent1" this can be metadata information for sequence component location
 *
 * @param value represents additional information associated with location
 */
case class Metadata(value: Map[String, String]) {
  // Used from java API
  def this(metadata: java.util.Map[String, String]) = this(metadata.asScala.toMap)

  def jMetadata: util.Map[String, String] = value.asJava

  def add(key: String, value: String): Metadata = copy(this.value + (key -> value))

  def withPid(pid: Long): Metadata                   = add(PidKey, pid.toString)
  def withAgentPrefix(agentPrefix: Prefix): Metadata = add(AgentPrefixKey, agentPrefix.toString)

  def get(key: String): Option[String] = value.get(key)
  def getPid: Option[Long]             = get(PidKey).map(_.toLong)
  def getAgentPrefix: Option[Prefix]   = get(AgentPrefixKey).map(Prefix(_))

  def jGet(key: String): Optional[String] = get(key).toJava
  def jGetPid: Optional[Long]             = jGet(PidKey).map(_.toLong)
  def jGetAgentPrefix: Optional[Prefix]   = jGet(AgentPrefixKey).map(Prefix(_))
}

object Metadata {
  private val PidKey         = "PID"
  private val AgentPrefixKey = "agentPrefix"
  def empty: Metadata        = Metadata(Map.empty)

  def apply(): Metadata = Metadata(Map.empty)
}
