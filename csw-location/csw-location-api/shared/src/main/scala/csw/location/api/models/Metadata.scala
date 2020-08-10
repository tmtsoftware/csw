package csw.location.api.models

import java.util

import scala.jdk.CollectionConverters._

/**
 * metadata represents any additional information (metadata) associated with location
 * For example, "agentId": "ESW.agent1" this can be metadata information for sequence component location
 *
 * @param metadata represents additional information associated with location
 */
case class Metadata(metadata: Map[String, String]) {
  // Used from java API
  def this(metadata: java.util.Map[String, String]) = this(metadata.asScala.toMap)

  def jMetadata: util.Map[String, String] = metadata.asJava
}

object Metadata {
  def empty: Metadata = Metadata(Map.empty)
}
