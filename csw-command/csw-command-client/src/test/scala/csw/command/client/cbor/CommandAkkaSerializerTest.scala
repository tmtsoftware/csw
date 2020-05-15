package csw.command.client.cbor

import java.nio.file.Files

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.serialization.SerializationExtension
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.ComponentCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorLifecycleState,
  LifecycleStateSubscription
}
import csw.command.client.messages.ContainerCommonMessage.{GetComponents, GetContainerLifecycleState}
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.messages._
import csw.command.client.messages.sequencer.SequencerMsg.{Query, QueryFinal, SubmitSequence}
import csw.command.client.models.framework.LockingResponse._
import csw.command.client.models.framework.PubSub.{Subscribe, SubscribeOnly, Unsubscribe}
import csw.command.client.models.framework.SupervisorLifecycleState._
import csw.command.client.models.framework.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.command.client.models.framework._
import csw.commons.ResourceReader
import csw.location.api.models.ComponentType
import csw.logging.models.{Level, LogMetadata}
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.KeyType.{ByteArrayKey, IntKey}
import csw.params.core.generics.{Key, Parameter}
import csw.params.core.models.Units.{coulomb, pascal}
import csw.params.core.models.{ArrayData, Id, ObsId}
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.prefix.models.Prefix
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class CommandAkkaSerializerTest extends AnyFunSuite with Matchers with BeforeAndAfterAll {

  private final implicit val system: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "example")
  private final val serialization                                       = SerializationExtension(system)
  private final val prefix                                              = Prefix("wfos.prog.cloudcover")

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  test("should use command serializer for CommandResponse (de)serialization") {
    val testData = Table(
      "CommandResponse models",
      Accepted(Id()),
      Started(Id()),
      Completed(Id(), Result.emptyResult),
      Invalid(Id(), CommandIssue.OtherIssue("test issue")),
      Error(Id(), "test"),
      Cancelled(Id()),
      Locked(Id())
    )

    forAll(testData) { commandResponse =>
      val serializer = serialization.findSerializerFor(commandResponse)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(commandResponse)
      serializer.fromBinary(bytes, Some(commandResponse.getClass)) shouldEqual commandResponse
    }
  }

  test("should use command serializer for StateVariable (de)serialization") {
    val testData = Table(
      "StateVariable models",
      CurrentState(prefix, StateName("filterwheel")),
      DemandState(prefix, StateName("filterwheel"))
    )

    forAll(testData) { stateVariable =>
      val serializer = serialization.findSerializerFor(stateVariable)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(stateVariable)
      serializer.fromBinary(bytes, Some(stateVariable.getClass)) shouldEqual stateVariable
    }
  }

  test("should use command serializer for messages (de)serialization") {
    val prefix = Prefix("wfos.prog.cloudcover")

    val intKey = IntKey.make("intKey")
    val param  = intKey.set(1, 2, 3).withUnits(coulomb)
    val setup  = Setup(prefix, CommandName("move"), Some(ObsId("Obs001"))).add(param)

    val keyName                            = "imageKey"
    val imageKey: Key[ArrayData[Byte]]     = ByteArrayKey.make(keyName)
    val imgPath                            = ResourceReader.copyToTmp("/smallBinary.bin", ".bin")
    val imgBytes                           = Files.readAllBytes(imgPath)
    val binaryImgData: ArrayData[Byte]     = ArrayData.fromArray(imgBytes)
    val param2: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal
    val observe                            = Observe(prefix, CommandName("move"), Some(ObsId("Obs001"))).add(param2)

    val submitResponseProbe   = TestProbe[SubmitResponse]()
    val onewayResponseProbe   = TestProbe[OnewayResponse]()
    val validateResponseProbe = TestProbe[ValidateResponse]()
    val queryResponseProbe    = TestProbe[SubmitResponse]()
    val lockingResponseProbe  = TestProbe[LockingResponse]()

    val lifecycleProbe                = TestProbe[LifecycleStateChanged]()
    val currentStateProbe             = TestProbe[CurrentState]()
    val supervisorLifecycleStateProbe = TestProbe[SupervisorLifecycleState]()

    val componentsProbe              = TestProbe[Components]()
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]()

    val logMetadataProbe = TestProbe[LogMetadata]()

    val testData = Table(
      "Command models",
      Submit(setup, submitResponseProbe.ref),
      Oneway(observe, onewayResponseProbe.ref),
      Validate(observe, validateResponseProbe.ref),
      Lock(prefix, lockingResponseProbe.ref, 1.seconds),
      Unlock(prefix, lockingResponseProbe.ref),
      Lifecycle(GoOffline),
      Lifecycle(GoOnline),
      Shutdown,
      Restart,
      LifecycleStateSubscription(Subscribe(lifecycleProbe.ref)),
      LifecycleStateSubscription(Unsubscribe(lifecycleProbe.ref)),
      LifecycleStateSubscription(SubscribeOnly(lifecycleProbe.ref, Set(StateName("filterwheel")))),
      ComponentStateSubscription(Subscribe(currentStateProbe.ref)),
      GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref),
      GetComponents(componentsProbe.ref),
      GetContainerLifecycleState(containerLifecycleStateProbe.ref),
      Query(Id(), queryResponseProbe.ref),
      GetComponentLogMetadata(logMetadataProbe.ref),
      SetComponentLogLevel(Level.WARN)
    )

    forAll(testData) { command =>
      val serializer = serialization.findSerializerFor(command)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(command)
      serializer.fromBinary(bytes, Some(command.getClass)) shouldEqual command
    }
  }

  test("should use command serializer for (de)serialize LockingResponse") {
    val testData = Table(
      "LockingResponse models",
      LockAcquired,
      AcquiringLockFailed(""),
      LockReleased,
      LockAlreadyReleased,
      ReleasingLockFailed(""),
      LockExpired,
      LockExpiringShortly
    )

    forAll(testData) { lockingResponse =>
      val serializer = serialization.findSerializerFor(lockingResponse)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(lockingResponse)
      serializer.fromBinary(bytes, Some(lockingResponse.getClass)) shouldEqual lockingResponse
    }
  }

  test("should use command serializer for (de)serialize LifecycleStateChanged") {
    val componentMessageProbe = TestProbe[ComponentMessage]()
    val lifecycleStateChanged = LifecycleStateChanged(componentMessageProbe.ref, Idle)

    val serializer = serialization.findSerializerFor(lifecycleStateChanged)
    serializer.getClass shouldBe classOf[CommandAkkaSerializer]

    val bytes = serializer.toBinary(lifecycleStateChanged)
    serializer.fromBinary(bytes, Some(lifecycleStateChanged.getClass)) shouldEqual lifecycleStateChanged
  }

  test("should use command serializer for (de)serialize SupervisorLifecycleState") {

    val testData = Table(
      "SupervisorLifecycleState models",
      Idle,
      Running,
      RunningOffline,
      SupervisorLifecycleState.Restart,
      SupervisorLifecycleState.Shutdown,
      SupervisorLifecycleState.Lock
    )

    forAll(testData) { supervisorLifecycleState =>
      val serializer = serialization.findSerializerFor(supervisorLifecycleState)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(supervisorLifecycleState)
      serializer.fromBinary(bytes, Some(supervisorLifecycleState.getClass)) shouldEqual supervisorLifecycleState
    }
  }

  test("should use command serializer for (de)serialize ContainerLifecycleState") {

    val testData = Table(
      "ContainerLifecycleState models",
      ContainerLifecycleState.Idle,
      ContainerLifecycleState.Running
    )

    forAll(testData) { containerLifecycleState =>
      val serializer = serialization.findSerializerFor(containerLifecycleState)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(containerLifecycleState)
      serializer.fromBinary(bytes, Some(containerLifecycleState.getClass)) shouldEqual containerLifecycleState
    }
  }

  test("should use command serializer for (de)serialize Components") {
    val componentMessageProbe = TestProbe[ComponentMessage]()

    val components =
      Components(
        Set(
          Component(
            componentMessageProbe.ref,
            ComponentInfo(
              prefix,
              ComponentType.HCD,
              "behavior-class-name",
              LocationServiceUsage.DoNotRegister
            )
          )
        )
      )

    val serializer = serialization.findSerializerFor(components)
    serializer.getClass shouldBe classOf[CommandAkkaSerializer]

    val bytes = serializer.toBinary(components)
    serializer.fromBinary(bytes, Some(components.getClass)) shouldEqual components
  }

  test("should use command serializer for (de)serialize SequencerMsg") {
    val submitResponseProbe      = TestProbe[SubmitResponse]()
    val queryResponseProbe       = TestProbe[SubmitResponse]()
    val command: SequenceCommand = Setup(Prefix("csw.move"), CommandName("c1"), Some(ObsId("obsId")))
    val sequence                 = Sequence(command)
    val sequenceId               = Id()

    val testData = Table(
      "SequencerMsg models",
      SubmitSequence(sequence, submitResponseProbe.ref),
      Query(sequenceId, queryResponseProbe.ref),
      QueryFinal(sequenceId, submitResponseProbe.ref)
    )

    forAll(testData) { sequencerMsg =>
      val serializer = serialization.findSerializerFor(sequencerMsg)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(sequencerMsg)
      serializer.fromBinary(bytes, Some(sequencerMsg.getClass)) shouldEqual sequencerMsg
    }
  }
}
