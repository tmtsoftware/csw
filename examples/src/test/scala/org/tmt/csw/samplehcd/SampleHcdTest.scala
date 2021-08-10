package org.tmt.csw.samplehcd

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.client.CommandServiceFactory
import csw.location.api.models.ComponentType.HCD
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Setup}
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{ObsId, Units}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.prefix.models.Subsystem.CSW
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.collection.mutable
import scala.concurrent.Await

//#setup
class SampleHcdTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyFunSuiteLike with BeforeAndAfterEach {
  import frameworkTestKit._

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("SampleHcdStandalone.conf"))
  }

  import scala.concurrent.duration._
  test("HCD should be locatable using Location Service") {
    val connection   = AkkaConnection(ComponentId(Prefix("CSW.samplehcd"), ComponentType.HCD))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    akkaLocation.connection shouldBe connection
  }
  //#setup

  //#subscribe
  test("should be able to subscribe to HCD events") {
    val counterEventKey = EventKey(Prefix("CSW.samplehcd"), EventName("HcdCounter"))
    val hcdCounterKey   = KeyType.IntKey.make("counter")

    val subscriber = eventService.defaultSubscriber

    // wait for a bit to ensure HCD has started and published an event
    Thread.sleep(2000)

    val subscriptionEventList = mutable.ListBuffer[Event]()
    subscriber.subscribeCallback(Set(counterEventKey), { ev => subscriptionEventList.append(ev) })

    // Sleep for 5 seconds, to allow HCD to publish events
    Thread.sleep(5000)

    // Q. Why expected count is either 3 or 4?
    // A. Total sleep = 7 seconds (2 + 5), subscriber listens for all the events between 2-7 seconds
    //  1)  If HCD publish starts at 1st second
    //      then events published at 1, 3, 5, 7, 9 etc. seconds
    //      In this case, subscriber will receive events at 2(initial), 3, 5, 7, i.e. total 4 events
    //  2)  If HCD publish starts at 1.5th seconds
    //      then events published at 1.5, 3.5, 5.5, 7.5, 9.5 etc. seconds
    //      In this case, subscriber will receive events at 2(initial), 3.5, 5.5, i.e. total 3 events
    val recEventsCount = subscriptionEventList.toList.size
    recEventsCount should (be(3) or be(4))

    // extract counter values to a List for comparison
    val counterList = subscriptionEventList.toList.map {
      case sysEv: SystemEvent if sysEv.contains(hcdCounterKey) => sysEv(hcdCounterKey).head
      case _                                                   => -1
    }

    // we don't know exactly how long HCD has been running when this test runs,
    // so we don't know what the first value will be,
    // but we know we should get three consecutive numbers
    val expectedCounterList = (0 to 2).toList.map(_ + counterList.head)

    counterList shouldBe expectedCounterList
  }
  //#subscribe

  //#submit
  implicit val typedActorSystem: ActorSystem[_] = actorSystem
  test("should be able to send sleep command to HCD") {
    import scala.concurrent.duration._
    implicit val sleepCommandTimeout: Timeout = Timeout(10000.millis)

    // Construct Setup command
    val sleepTimeKey: Key[Long]         = KeyType.LongKey.make("SleepTime")
    val sleepTimeParam: Parameter[Long] = sleepTimeKey.set(5000).withUnits(Units.millisecond)
    val setupCommand                    = Setup(Prefix("csw.move"), CommandName("sleep"), Some(ObsId("2020A-001-123"))).add(sleepTimeParam)

    val connection = AkkaConnection(ComponentId(Prefix(CSW, "samplehcd"), ComponentType.HCD))

    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    val hcd = CommandServiceFactory.make(akkaLocation)
    // submit command and handle response
    val responseF = hcd.submitAndWait(setupCommand)

    Await.result(responseF, 10000.millis) shouldBe a[Completed]
  }
  //#submit

  //#exception
  test("should get timeout exception if submit timeout is too small") {
    import scala.concurrent.duration._
    implicit val sleepCommandTimeout: Timeout = Timeout(1000.millis)

    // Construct Setup command
    val sleepTimeKey: Key[Long]         = KeyType.LongKey.make("SleepTime")
    val sleepTimeParam: Parameter[Long] = sleepTimeKey.set(5000).withUnits(Units.millisecond)
    val setupCommand                    = Setup(Prefix("csw.move"), CommandName("sleep"), Some(ObsId("2020A-001-123"))).add(sleepTimeParam)

    val connection = AkkaConnection(ComponentId(Prefix(CSW, "samplehcd"), ComponentType.HCD))

    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    val hcd = CommandServiceFactory.make(akkaLocation)

    // submit command and handle response
    intercept[java.util.concurrent.TimeoutException] {
      val responseF = hcd.submitAndWait(setupCommand)
      Await.result(responseF, 10000.millis) shouldBe a[Completed]
    }
  }
  //#exception

  test("should be able to spawn a hcd without providing config file") {
    //#spawn-hcd
    spawnHCD(Prefix("TCS.sampleHcd"), (ctx, cswCtx) => new SampleHcdHandlers(ctx, cswCtx))
    //#spawn-hcd

    val hcdConnection = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "sampleHcd"), HCD))
    val hcdLocation   = Await.result(locationService.resolve(hcdConnection, 5.seconds), 10.seconds)
    hcdLocation.value.connection shouldBe hcdConnection
  }
}
