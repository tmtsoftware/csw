package csw.param

import java.util.UUID

/**
 * Implementation of unique id for each running command (returned from a queue submit).
 */
object RunId {
  def apply(): RunId  = new RunId(UUID.randomUUID().toString)
  def create(): RunId = new RunId(UUID.randomUUID().toString)

  def apply(uuid: UUID): RunId = new RunId(uuid.toString)
}

case class RunId(id: String)
