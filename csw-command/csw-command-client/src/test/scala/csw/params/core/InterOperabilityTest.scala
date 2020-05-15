package csw.params.core

import java.util.Optional

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.params.commands.{Command, CommandName, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.ObsId
import csw.params.events.SystemEvent
import csw.prefix.models.Prefix
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

case class CommandMsg(
    command: Command,
    ackTo: ActorRef[java.util.Set[Parameter[_]]],
    replyTo: ActorRef[SystemEvent],
    obsIdAck: ActorRef[Optional[ObsId]]
)

// DEOPSCSW-184: Change configurations - attributes and values
class InterOperabilityTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val prefixStr    = "wfos.red.detector"
  private val obsId: ObsId = ObsId("Obs001")
  private val intKey       = KeyType.IntKey.make("intKey")
  private val stringKey    = KeyType.StringKey.make("stringKey")
  private val intParam     = intKey.set(22, 33)
  private val stringParam  = stringKey.set("First", "Second")

  private implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "test")
  implicit val testKit: TestKitSettings       = TestKitSettings(system)

  private val scalaSetup = Setup(Prefix(prefixStr), CommandName(prefixStr), Some(obsId)).add(intParam).add(stringParam)

  private val jCommandHandlerActor: ActorRef[CommandMsg] =
    system.systemActorOf[CommandMsg](JavaCommandHandler.behavior(), "javaCommandHandler")

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }

  // 1. sends scala Setup command to Java Actor
  // 2. onMessage, Java actor extracts paramSet from Setup command and replies back to scala actor
  // 3. also, java actor creates StatusEvent and forward it to scala actor
  test("should able to send commands/events from scala code to java and vice a versa | DEOPSCSW-184") {
    val ackProbe     = TestProbe[java.util.Set[Parameter[_]]]()
    val replyToProbe = TestProbe[SystemEvent]()
    val obsIdProbe   = TestProbe[Optional[ObsId]]()

    jCommandHandlerActor ! CommandMsg(scalaSetup, ackProbe.ref, replyToProbe.ref, obsIdProbe.ref)

    val set = ackProbe.expectMessageType[java.util.Set[Parameter[_]]]
    set.asScala.toSet shouldBe Set(intParam, stringParam)

    val eventFromJava = replyToProbe.expectMessageType[SystemEvent]
    eventFromJava.paramSet shouldBe Set(JavaCommandHandler.encoderParam, JavaCommandHandler.epochStringParam)

    obsIdProbe.expectMessageType[Optional[ObsId]]
  }

}
