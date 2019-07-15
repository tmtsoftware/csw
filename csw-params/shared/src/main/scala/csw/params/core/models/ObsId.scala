package csw.params.core.models

import java.util.Optional

object ObsId {

  /**
   * Represents an empty ObsId
   *
   * @return an ObsId with empty string
   */
  def empty: ObsId = ObsId("")
}

/**
 * Represents a unique observation id
 *
 * @param obsId the string representation of obsId
 */
case class ObsId(obsId: String) {

  /**
   * Returns the ObsId in form of Option
   *
   * @return a defined Option with obsId
   */
  def asOption: Option[ObsId] = Some(new ObsId(obsId))

  /**
   * Returns the ObsId in form of Optional
   *
   * @return a defined Optional with obsId
   */
  def asOptional: Optional[ObsId] = Optional.of(new ObsId(obsId))
}
