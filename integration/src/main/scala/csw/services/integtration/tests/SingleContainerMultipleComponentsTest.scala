package csw.services.integtration.tests

import akka.actor.{ActorPath, Props}
import akka.serialization.Serialization
import csw.services.integtration.apps.TromboneHCD
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.services.location.scaladsl.{ActorRuntime, LocationService, LocationServiceFactory}
import org.scalatest.{FunSuite, Matchers}

class SingleContainerMultipleComponentsTest extends FunSuite with Matchers {
  private val actorRuntime = new ActorRuntime("crdt")
  private val locationService: LocationService = LocationServiceFactory.make(actorRuntime)

  test("should be able to register and discover multiple components in single container") {
    val tromboneHcdActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection = AkkaConnection(componentId)

    val tromboneHcd1ActorRef = actorRuntime.actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd1")
    val componentId1 = ComponentId("trombonehcd1", ComponentType.HCD)
    val connection1 = AkkaConnection(componentId1)

    val actorPath = ActorPath.fromString(Serialization.serializedActorPath(tromboneHcdActorRef))
    val registration = new AkkaLocation(connection, tromboneHcdActorRef)

    val actorPath1 = ActorPath.fromString(Serialization.serializedActorPath(tromboneHcd1ActorRef))
    val registration1 = new AkkaLocation(connection1, tromboneHcd1ActorRef)

    val registrationResult = locationService.register(registration).await
    val registrationResult1 = locationService.register(registration1).await

    val location: Option[Location] = locationService.resolve(connection).await
    val location1: Option[Location] = locationService.resolve(connection1).await

    location.get shouldBe(registration)
    location1.get shouldBe(registration1)
    actorRuntime.terminate()
  }
}
