package example.location

import java.net.InetAddress

import akka.actor.typed
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.ActorMaterializer
import akka.stream.typed.scaladsl
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.Connection.AkkaConnection
import csw.location.model.{ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.{LoggerFactory, LoggingSystemFactory}
import csw.params.core.models.Prefix

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * An example that shows how to register a component actor with the location service.
 */
object LocationServiceExampleComponentApp extends App {
  implicit val system: typed.ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "example-system")
  implicit val mat: ActorMaterializer                   = scaladsl.ActorMaterializer()
  private val locationService                           = HttpLocationServiceFactory.makeLocalClient

  //#create-logging-system
  private val host = InetAddress.getLocalHost.getHostName
  LoggingSystemFactory.start("LocationServiceExampleComponent", "0.1", host, system)
  //#create-logging-system

  system.spawn(LocationServiceExampleComponent.behaviour(locationService), LocationServiceExampleComponent.connection.name)
}

object LocationServiceExampleComponent {
  // Component ID of the service
  val componentId = ComponentId("LocationServiceExampleComponent", ComponentType.Assembly)

  // Connection for the service
  val connection = AkkaConnection(componentId)

  // Message sent from client once location has been resolved
  case class ClientMessage(replyTo: typed.ActorRef[_])

  def behaviour(locationService: LocationService): Behaviors.Receive[ClientMessage] =
    Behaviors.receive[ClientMessage]((ctx, msg) => {
      val log: Logger = new LoggerFactory("my-component-name").getLogger(ctx)
      log.info("In actor LocationServiceExampleComponent")
      // Register with the location service
      val registrationResult: Future[RegistrationResult] =
        locationService.register(
          AkkaRegistrationFactory.make(
            LocationServiceExampleComponent.connection,
            Prefix("nfiraos.ncc.trombone"),
            ctx.self.toURI
          )
        )
      Await.result(registrationResult, 5.seconds)

      log.info("LocationServiceExampleComponent registered.")
      msg match {
        case ClientMessage(replyTo) =>
          log.info(s"Received scala client message from: $replyTo")
          Behaviors.same
      }
    })
}
