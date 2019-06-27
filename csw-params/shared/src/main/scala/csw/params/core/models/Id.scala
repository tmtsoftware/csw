package csw.params.core.models

import java.util.UUID

/**
 * Implementation of unique id fulfilling TMT requirements (returned from a queue submit).
 *
 * @param id a string representation of unique id
 */
case class Id(id: String)

object Id {

  /**
   * A helper method to create Id with random unique id generator
   *
   * @return an instance of Id
   */
  def apply(): Id = new Id(UUID.randomUUID().toString)
}
