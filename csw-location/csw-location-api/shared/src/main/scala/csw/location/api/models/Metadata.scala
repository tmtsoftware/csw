package csw.location.api.models

import java.util
import java.util.Optional

import csw.location.api.models.Metadata.{AgentPrefixKey, PIDKey}

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

  def add(k: String, v: String): Metadata = copy(value + (k -> v))

  def withPID(pid: String): Metadata           = add(PIDKey, pid)
  def withAgent(agentPrefix: String): Metadata = add(AgentPrefixKey, agentPrefix)

  def get(k: String): Option[String] = value.get(k)
  def getPID: Option[String]         = get(PIDKey)
  def getAgentPrefix: Option[String] = get(AgentPrefixKey)

  def jGet(k: String): Optional[String] = get(k).toJava
  def jGetPID: Optional[String]         = jGet(PIDKey)
  def jGetAgentPrefix: Optional[String] = jGet(AgentPrefixKey)
}

object Metadata {
  private val PIDKey         = "PID"
  private val AgentPrefixKey = "agentPrefix"
  def empty: Metadata        = Metadata(Map.empty)

  def apply(): Metadata = Metadata(Map.empty)
}
