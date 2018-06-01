package csw.messages.params.generics

import java.nio.file.{Files, Paths}
import java.time.Instant

import akka.actor.{typed, ActorSystem}
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.SerializationExtension
import akka.testkit.typed.scaladsl.TestProbe
import com.twitter.chill.akka.AkkaSerializer
import csw.commons.tagobjects.FileSystemSensitive
import csw.messages.commands.CommandResponse._
import csw.messages.commands.{CommandIssue, _}
import csw.messages.events.{EventName, EventTime, ObserveEvent, SystemEvent}
import csw.messages.framework.LocationServiceUsage.DoNotRegister
import csw.messages.framework.PubSub.Subscribe
import csw.messages.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.messages.framework._
import csw.messages.location.ComponentType.HCD
import csw.messages.location.Connection
import csw.messages.params.generics.KeyType.{ByteArrayKey, ChoiceKey, DoubleMatrixKey, IntKey, StructKey}
import csw.messages.params.models.Units.{arcmin, coulomb, encoder, joule, lightyear, meter, pascal, NoUnits}
import csw.messages.params.models._
import csw.messages.params.states.{CurrentState, DemandState, StateName}
import csw.messages.scaladsl.ComponentCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorLifecycleState,
  LifecycleStateSubscription
}
import csw.messages.scaladsl.ComponentMessage
import csw.messages.scaladsl.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.messages.scaladsl.RunningMessage.Lifecycle
import csw.messages.scaladsl.SupervisorContainerCommonMessages.{Restart, Shutdown}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-187: Efficient serialization to/from binary
class AkkaKryoSerializationTest extends FunSpec with Matchers with BeforeAndAfterAll {
  private final val system        = ActorSystem("example")
  private final val serialization = SerializationExtension(system)
  private final val prefix        = Prefix("wfos.prog.cloudcover")

  override protected def afterAll(): Unit = Await.result(system.terminate(), 2.seconds)

  describe("Test akka serialization of Commands") {

    it("should serialize Setup") {
      val intKey = IntKey.make("intKey")
      val param  = intKey.set(1, 2, 3).withUnits(coulomb)

      val setup           = Setup(prefix, CommandName("move"), Some(ObsId("Obs001"))).add(param)
      val setupSerializer = serialization.findSerializerFor(setup)

      setupSerializer.getClass shouldBe classOf[AkkaSerializer]

      val setupBytes: Array[Byte] = setupSerializer.toBinary(setup)
      setupSerializer.fromBinary(setupBytes) shouldBe setup
    }

    it("should serialize Observe", FileSystemSensitive) {
      val keyName                        = "imageKey"
      val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make(keyName)

      val imgPath  = Paths.get(getClass.getResource("/smallBinary.bin").getPath)
      val imgBytes = Files.readAllBytes(imgPath)

      val binaryImgData: ArrayData[Byte]    = ArrayData.fromArray(imgBytes)
      val param: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal

      val observe           = Observe(prefix, CommandName("move"), Some(ObsId("Obs001"))).add(param)
      val observeSerializer = serialization.findSerializerFor(observe)

      observeSerializer.getClass shouldBe classOf[AkkaSerializer]

      val observeBytes: Array[Byte] = observeSerializer.toBinary(observe)
      observeSerializer.fromBinary(observeBytes) shouldBe observe
    }

    it("should serialize Wait") {
      val keyName                  = "matrixKey"
      val m1: Array[Array[Double]] = Array(Array(1, 2, 3), Array(4, 5, 6), Array(7, 8, 9))
      val m2: Array[Array[Double]] = Array(Array(1, 2, 3, 4, 5), Array(10, 20, 30, 40, 50))

      val matrixData1     = MatrixData.fromArrays(m1)
      val matrixData2     = MatrixData.fromArrays(m2)
      val doubleMatrixKey = DoubleMatrixKey.make(keyName)
      val param           = doubleMatrixKey.set(Array(matrixData1, matrixData2), lightyear)

      val wait: Wait     = Wait(prefix, CommandName("move"), Some(ObsId("Obs001"))).add(param)
      val waitSerializer = serialization.findSerializerFor(wait)

      waitSerializer.getClass shouldBe classOf[AkkaSerializer]

      val waitBytes: Array[Byte] = waitSerializer.toBinary(wait)
      waitSerializer.fromBinary(waitBytes) shouldBe wait
    }
  }

  describe("Test akka serialization of Events") {

    it("should serialize ObserveEvent") {
      val jupiter   = Choice("Jupiter")
      val saturn    = Choice("Saturn")
      val pluto     = Choice("Pluto")
      val choiceKey = ChoiceKey.make("choiceKey", jupiter, saturn, pluto)

      val param = choiceKey.set(jupiter, pluto).withUnits(arcmin)

      val observeEvent: ObserveEvent = ObserveEvent(Id(), prefix, EventName("filter wheel"), EventTime(), Set.empty).add(param)
      val observeEventSerializer     = serialization.findSerializerFor(observeEvent)

      observeEventSerializer.getClass shouldBe classOf[AkkaSerializer]

      val observeEventBytes: Array[Byte] = observeEventSerializer.toBinary(observeEvent)
      observeEventSerializer.fromBinary(observeEventBytes) shouldBe observeEvent
    }

    it("should serialize SystemEvent") {
      val structKey = StructKey.make("structKey")

      val ra     = KeyType.StringKey.make("ra")
      val dec    = KeyType.StringKey.make("dec")
      val epoch  = KeyType.DoubleKey.make("epoch")
      val struct = Struct().madd(ra.set("12:13:14.1"), dec.set("32:33:34.4"), epoch.set(1950.0))

      val param = structKey.set(struct).withUnits(joule)

      val systemEvent: SystemEvent = SystemEvent(Id(), prefix, EventName("filter wheel"), EventTime(), Set.empty).add(param)
      val systemEventSerializer    = serialization.findSerializerFor(systemEvent)

      systemEventSerializer.getClass shouldBe classOf[AkkaSerializer]

      val systemEventBytes: Array[Byte] = systemEventSerializer.toBinary(systemEvent)
      systemEventSerializer.fromBinary(systemEventBytes) shouldBe systemEvent
    }
  }

  describe("Test akka serialization of StateVariables") {

    it("should serialize CurrentState") {
      val charKey        = KeyType.CharKey.make("charKey")
      val intArrayKey    = KeyType.IntArrayKey.make("intArrayKey")
      val a1: Array[Int] = Array(1, 2, 3, 4, 5)
      val a2: Array[Int] = Array(10, 20, 30, 40, 50)

      val charParam     = charKey.set('A', 'B', 'C').withUnits(encoder)
      val intArrayParam = intArrayKey.set(a1, a2).withUnits(meter)

      val currentState           = CurrentState(prefix, StateName("testStateName")).madd(charParam, intArrayParam)
      val currentStateSerializer = serialization.findSerializerFor(currentState)

      currentStateSerializer.getClass shouldBe classOf[AkkaSerializer]

      val currentStateBytes: Array[Byte] = currentStateSerializer.toBinary(currentState)
      currentStateSerializer.fromBinary(currentStateBytes) shouldBe currentState
    }

    it("should serialize DemandState") {
      val charKey      = KeyType.CharKey.make("charKey")
      val intKey       = KeyType.IntKey.make("intKey")
      val booleanKey   = KeyType.BooleanKey.make("booleanKey")
      val timestampKey = KeyType.TimestampKey.make("timestampKey")

      val charParam    = charKey.set('A', 'B', 'C').withUnits(NoUnits)
      val intParam     = intKey.set(1, 2, 3).withUnits(meter)
      val booleanParam = booleanKey.set(true, false)
      val timestamp    = timestampKey.set(Instant.now)

      val demandState           = DemandState(prefix, StateName("testStateName")).madd(charParam, intParam, booleanParam, timestamp)
      val demandStateSerializer = serialization.findSerializerFor(demandState)

      demandStateSerializer.getClass shouldBe classOf[AkkaSerializer]

      val demandStateBytes: Array[Byte] = demandStateSerializer.toBinary(demandState)
      demandStateSerializer.fromBinary(demandStateBytes) shouldBe demandState
    }
  }

  describe("csw messages") {
    implicit val typedSystem: typed.ActorSystem[Nothing] = system.toTyped

    it("should serialize ToComponentLifecycle messages") {
      serialization.findSerializerFor(GoOffline).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(GoOnline).getClass shouldBe classOf[AkkaSerializer]
    }

    it("should serialize Lifecycle messages") {
      serialization.findSerializerFor(Lifecycle(GoOffline)).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(Lifecycle(GoOnline)).getClass shouldBe classOf[AkkaSerializer]
    }

    it("should serialize Common messages for all components") {
      serialization.findSerializerFor(Shutdown).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(Restart).getClass shouldBe classOf[AkkaSerializer]
    }

    it("should serialize Common messages for supervisor") {
      val lifecycleProbe    = TestProbe[LifecycleStateChanged]
      val currentStateProbe = TestProbe[CurrentState]
      val supStateProbe     = TestProbe[SupervisorLifecycleState]

      val lifecycleStateSubscription      = LifecycleStateSubscription(Subscribe(lifecycleProbe.ref))
      val currentStateSubscription        = ComponentStateSubscription(Subscribe(currentStateProbe.ref))
      val supervisorLifecycleStateMessage = GetSupervisorLifecycleState(supStateProbe.ref)

      serialization.findSerializerFor(lifecycleStateSubscription).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(currentStateSubscription).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(supervisorLifecycleStateMessage).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(SupervisorLifecycleState.Idle).getClass shouldBe classOf[AkkaSerializer]
    }

    it("should serialize Common messages for container") {
      val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]
      val componentsProbe              = TestProbe[Components]
      val supExtMsgProbe               = TestProbe[ComponentMessage]
      val connection                   = Connection.from("Trombone-hcd-akka")
      val componentInfo = ComponentInfo(
        "name",
        HCD,
        "prefix",
        "className",
        DoNotRegister,
        Set(connection),
        10.seconds
      )
      val component  = Component(supExtMsgProbe.ref, componentInfo)
      val components = Components(Set(component))

      val getComponentsMessage              = GetComponents(componentsProbe.ref)
      val getContainerLifecycleStateMessage = GetContainerLifecycleState(containerLifecycleStateProbe.ref)

      serialization.findSerializerFor(getComponentsMessage).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(getContainerLifecycleStateMessage).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(components).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(ContainerLifecycleState.Idle).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(ContainerLifecycleState.Running).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(component).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(componentInfo).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(componentInfo.componentType).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(componentInfo.locationServiceUsage).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(componentInfo.connections.head).getClass shouldBe classOf[AkkaSerializer]
      serialization.findSerializerFor(connection.componentId).getClass shouldBe classOf[AkkaSerializer]
    }

    it("should serialize CommandValidationResponse messages") {
      serialization.findSerializerFor(Accepted(Id())).getClass shouldBe classOf[AkkaSerializer]
      serialization
        .findSerializerFor(Invalid(Id(), CommandIssue.OtherIssue("test issue")))
        .getClass shouldBe classOf[AkkaSerializer]
    }

    it("should serialize CommandExecutionResponse messages") {
      val testData = Table(
        "CommandExecutionResponse models",
        CompletedWithResult(Id(), Result(prefix)),
        NoLongerValid(Id(), CommandIssue.OtherIssue("test issue")),
        Completed(Id()),
        Error(Id(), "test"),
        Cancelled(Id())
      )

      forAll(testData) { commandResponse ⇒
        serialization
          .findSerializerFor(commandResponse)
          .getClass shouldBe classOf[AkkaSerializer]

      }
    }
  }
}
