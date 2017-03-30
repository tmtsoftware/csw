package csw.services.location.javadsl

import java.util.Optional
import java.util.concurrent.CompletionStage

import akka.Done
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.services.location.models._
import java.{util => ju}

trait ILocationService {
  /**
    * @param registration object
    * returns registration-result which can be used to unregister
    */
  def register(registration: Registration): CompletionStage[IRegistrationResult]

  def unregister(connection: Connection): CompletionStage[Done]

  def unregisterAll(): CompletionStage[Done]

  def resolve(connection: Connection): CompletionStage[Optional[Location]]

  def list: CompletionStage[ju.List[Location]]

  def list(componentType: ComponentType): CompletionStage[ju.List[Location]]

  def list(hostname: String): CompletionStage[ju.List[Location]]

  def list(connectionType: ConnectionType): CompletionStage[ju.List[Location]]

  def track(connection: Connection): Source[TrackingEvent, KillSwitch]

  def shutdown(): CompletionStage[Done]
}
