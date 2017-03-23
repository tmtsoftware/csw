package csw.services.location.javadsl

import java.util.concurrent.CompletionStage

import akka.Done
import akka.stream.KillSwitch
import akka.stream.javadsl.Source
import csw.services.location.models._
import java.{util => ju}

trait ILocationService {
  /**
    * @param location object
    * returns registration-result which can be used to unregister
    */
  def register(location: Resolved): CompletionStage[RegistrationResult]

  def unregister(connection: Connection): CompletionStage[Done]

  def unregisterAll(): CompletionStage[Done]

  def resolve(connection: Connection): CompletionStage[Option[Resolved]]

  def list: CompletionStage[ju.List[Resolved]]

  def list(componentType: ComponentType): CompletionStage[ju.List[Resolved]]

  def list(hostname: String): CompletionStage[ju.List[Resolved]]

  def list(connectionType: ConnectionType): CompletionStage[ju.List[Resolved]]

  def track(connection: Connection): Source[TrackingEvent, KillSwitch]
}
