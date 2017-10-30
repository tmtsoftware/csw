package csw.messages.params

import akka.actor
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.messages.ccs.commands.{Command, Setup}
import csw.messages.ccs.events.StatusEvent
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.{ObsId, Prefix, RunId}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

case class CommandMsg(command: Command, ackTo: ActorRef[java.util.Set[Parameter[_]]], replyTo: ActorRef[StatusEvent])

// DEOPSCSW-184: Change configurations - attributes and values
class InterOperabilityTest extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val timeout: Timeout = Timeout(5.seconds)

  private val prefixStr    = "wfos.red.detector"
  private val runId: RunId = RunId()
  private val obsId: ObsId = ObsId("Obs001")
  private val intKey       = KeyType.IntKey.make("intKey")
  private val stringKey    = KeyType.StringKey.make("stringKey")
  private val intParam     = intKey.set(22, 33)
  private val stringParam  = stringKey.set("First", "Second")

  private val system: actor.ActorSystem          = actor.ActorSystem("test")
  implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
  implicit val testKit: TestKitSettings          = TestKitSettings(typedSystem)

  private val scalaSetup = Setup(runId, obsId, Prefix(prefixStr)).add(intParam).add(stringParam)

  private val javaCmdHandlerBehavior: Future[ActorRef[CommandMsg]] =
    typedSystem.systemActorOf[CommandMsg](JavaCommandHandler.behavior(), "javaCommandHandler")

  private val jCommandHandlerActor: ActorRef[CommandMsg] = Await.result(javaCmdHandlerBehavior, 5.seconds)

  override protected def afterAll(): Unit = Await.result(system.terminate(), 5.seconds)

  // 1. sends scala Setup command to Java Actor
  // 2. onMessage, Java actor extracts paramSet from Setup command and replies back to scala actor
  // 3. also, java actor creates StatusEvent and forward it to scala actor
  test("should able to send commands/events from scala code to java and vice a versa") {
    val ackProbe     = TestProbe[java.util.Set[Parameter[_]]]
    val replyToProbe = TestProbe[StatusEvent]

    jCommandHandlerActor ! CommandMsg(scalaSetup, ackProbe.ref, replyToProbe.ref)

    val set = ackProbe.expectMsgType[java.util.Set[Parameter[_]]]
    set.asScala.toSet shouldBe Set(intParam, stringParam)

    val eventFromJava = replyToProbe.expectMsgType[StatusEvent]
    eventFromJava.paramSet shouldBe Set(JavaCommandHandler.encoderParam, JavaCommandHandler.epochStringParam)
  }

}
