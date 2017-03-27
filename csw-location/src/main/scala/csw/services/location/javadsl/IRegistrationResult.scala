package csw.services.location.javadsl

import akka.Done
import csw.services.location.models.Location
import java.util.concurrent.CompletionStage

trait IRegistrationResult {
  def unregister: CompletionStage[Done]

  def location: Location
}
