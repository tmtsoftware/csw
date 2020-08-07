package csw.location.api.models

/**
 * metadata represents any additional information (metadata) associated with location
 * For example, "agentId": "ESW.agent1" this can be metadata information for sequence component location
 *
  * @param metadata represents additional information associated with location
 */
case class Metadata(metadata: Map[String, String])

object Metadata {
  def empty: Metadata = Metadata(Map.empty)
}
