package csw.services.integtration.tests

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{typed, ActorSystem, Props, Scheduler}
import akka.util.Timeout
import csw.messages.CommandMessage.Submit
import csw.messages.commands.{CommandName, Setup}
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.messages.location.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.api.exceptions.OtherLocationIsRegistered
import csw.services.location.api.models.AkkaRegistration
import csw.services.location.api.scaladsl.LocationService
import csw.messages.location.{AkkaLocation, ComponentId, ComponentType, HttpLocation}
import csw.messages.params.models.Prefix
import csw.services.command.extensions.AkkaLocationExt.RichAkkaLocation
import csw.services.integtration.apps.TromboneHCD
import csw.services.integtration.common.TestFutureExtension.RichFuture
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.messages.LogControlMessages
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class LocationServiceIntegrationTest extends FunSuite with Matchers with BeforeAndAfter with BeforeAndAfterAll {

  implicit val actorSystem: ActorSystem                = ClusterAwareSettings.system
  val locationService: LocationService                 = LocationServiceFactory.withSystem(actorSystem)
  implicit val typedSystem: typed.ActorSystem[Nothing] = actorSystem.toTyped
  implicit val sched: Scheduler                        = actorSystem.scheduler
  implicit val timeout: Timeout                        = Timeout(5.seconds)
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(typedSystem)

  override protected def afterAll(): Unit =
    Await.result(locationService.shutdown(TestFinishedReason), 5.seconds)

  test("should not allow duplicate akka registration") {
    val tromboneHcdActorRef                                  = actorSystem.actorOf(Props[TromboneHCD], "trombone-hcd")
    val logAdminActorRef: typed.ActorRef[LogControlMessages] = actorSystem.spawn(Behavior.empty, "trombone-admin")
    val componentId                                          = ComponentId("trombonehcd", ComponentType.HCD)
    val connection                                           = AkkaConnection(componentId)

    val registration = AkkaRegistration(connection, Prefix("nfiraos.ncc.trombone"), tromboneHcdActorRef, logAdminActorRef)
    Thread.sleep(4000)
    intercept[OtherLocationIsRegistered] {
      locationService.register(registration).await
    }
  }

  test("should able to resolve and communicate with remote HCD started on another container") {
    val componentId = ComponentId("trombonehcd", ComponentType.HCD)
    val connection  = AkkaConnection(componentId)
    val hcdLocation = locationService.find(connection).await.get

    hcdLocation shouldBe a[AkkaLocation]
    hcdLocation.connection shouldBe connection

    val hcdAkkaLocation = hcdLocation.asInstanceOf[AkkaLocation]

    hcdAkkaLocation.componentRef ! Submit(Setup(Prefix("wfos.prog.cloudcover"), CommandName("Unregister"), None), TestProbe().ref)
    Thread.sleep(3000)

    locationService.list.await should have size 1
  }

  test("list all components") {
    val listOfLocations = locationService.list.await

    listOfLocations should have size 1
  }

  test("should able to resolve remote Service started on another container") {

    val componentId = ComponentId("redisservice", ComponentType.Service)
    val connection  = HttpConnection(componentId)

    val hcdLocation = locationService.find(connection).await.get

    hcdLocation shouldBe a[HttpLocation]
    hcdLocation.connection shouldBe connection
  }
}
