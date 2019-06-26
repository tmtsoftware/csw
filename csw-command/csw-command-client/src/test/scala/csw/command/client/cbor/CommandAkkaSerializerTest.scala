package csw.command.client.cbor

import akka.actor.typed
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.serialization.SerializationExtension
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, Result}
import csw.params.core.models.{Id, Prefix}
import csw.params.core.states.{CurrentState, DemandState, StateName}
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class CommandAkkaSerializerTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private final val system        = typed.ActorSystem(Behavior.empty, "example")
  private final val serialization = SerializationExtension(system.toUntyped)
  private final val prefix        = Prefix("wfos.prog.cloudcover")

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  test("should use command serializer for CommandResponse (de)serialization") {
    val testData = Table(
      "CommandResponse models",
      Accepted(Id()),
      CompletedWithResult(Id(), Result(prefix)),
      Invalid(Id(), CommandIssue.OtherIssue("test issue")),
      Completed(Id()),
      Error(Id(), "test"),
      Cancelled(Id())
    )

    forAll(testData) { commandResponse ⇒
      val serializer = serialization.findSerializerFor(commandResponse)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(commandResponse)
      serializer.fromBinary(bytes, Some(commandResponse.getClass)) shouldEqual commandResponse
    }
  }

  //TODO: fix me
  ignore("should use command serializer for StateVariable (de)serialization") {
    val testData = Table(
      "StateVariable models",
      CurrentState(prefix, StateName("filterwheel")),
      DemandState(prefix, StateName("filterwheel"))
    )

    forAll(testData) { stateVariable ⇒
      val serializer = serialization.findSerializerFor(stateVariable)
      serializer.getClass shouldBe classOf[CommandAkkaSerializer]

      val bytes = serializer.toBinary(stateVariable)
      serializer.fromBinary(bytes, Some(stateVariable.getClass)) shouldEqual stateVariable
    }
  }
}
