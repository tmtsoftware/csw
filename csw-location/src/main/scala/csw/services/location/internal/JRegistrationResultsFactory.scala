package csw.services.location.internal

import java.util.concurrent.CompletionStage

import akka.Done
import csw.services.location.javadsl.IRegistrationResult
import csw.services.location.models.{Location, RegistrationResult}

import scala.compat.java8.FutureConverters.FutureOps

object JRegistrationResultsFactory {
  def from(registrationResult: RegistrationResult): IRegistrationResult = new IRegistrationResult {
    override def unregister: CompletionStage[Done] = registrationResult.unregister().toJava
    override def location: Location = registrationResult.location
  }
}
