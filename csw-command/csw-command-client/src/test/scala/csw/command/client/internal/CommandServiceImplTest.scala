package csw.command.client.internal

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.api.DemandMatcher
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.ComponentCommonMessage.ComponentStateSubscription
import csw.command.client.messages.{ComponentMessage, Query, QueryFinal}
import csw.command.client.models.framework.PubSub.Subscribe
import csw.commons.{AskProxyTestKit, RandomUtils}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, DemandState, StateName}
import csw.prefix.models.Prefix
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
import scala.util.Random

class CommandServiceImplTest extends AnyFunSuiteLike with Matchers with MockitoSugar with ScalaFutures with BeforeAndAfterAll {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-command-system")
  private implicit val timeout: Timeout                           = Timeout(10.seconds)

  private val askProxyTestKit = new AskProxyTestKit[ComponentMessage, CommandServiceImpl] {
    override def make(actorRef: ActorRef[ComponentMessage]): CommandServiceImpl = {
      new CommandServiceImpl(actorRef)
    }
  }
  import askProxyTestKit._

  private def randomString5 = RandomUtils.randomString5()

  private val prefix  = Prefix(s"csw.$randomString5")
  private val setup   = Setup(prefix, CommandName(randomString5), None)
  private val observe = Observe(prefix, CommandName(randomString5), None)
  private val id      = Id()

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
    system.whenTerminated.futureValue
  }

  test("should send validate message | CSW-110") {
    val validateResponse = mock[ValidateResponse]
    withBehavior { case Validate(`setup`, replyTo) =>
      replyTo ! validateResponse
    } check { cs =>
      cs.validate(setup).futureValue should ===(validateResponse)
    }
  }

  test("should submit command and wait for final response | CSW-110") {
    val submitResponse     = Started(id)
    val queryFinalResponse = mock[SubmitResponse]
    withBehavior {
      case Submit(`setup`, replyTo)  => replyTo ! submitResponse
      case QueryFinal(`id`, replyTo) => replyTo ! queryFinalResponse
    } check { cs =>
      cs.submitAndWait(setup).futureValue should ===(queryFinalResponse)
    }
  }

  test("should submit command | CSW-110") {
    val submitResponse = mock[SubmitResponse]
    withBehavior { case Submit(`setup`, replyTo) =>
      replyTo ! submitResponse
    } check { cs =>
      cs.submit(setup).futureValue should ===(submitResponse)
    }
  }

  test("should submit all commands and wait for final response | CSW-110") {
    val submitResponse     = Started(id)
    val queryFinalResponse = mock[SubmitResponse]
    withBehavior {
      case Submit(`setup`, replyTo)  => replyTo ! submitResponse
      case QueryFinal(`id`, replyTo) => replyTo ! queryFinalResponse
    } check { cs =>
      cs.submitAllAndWait(List(setup, observe)).futureValue should ===(List(queryFinalResponse))
    }
  }

  test("should send oneway command | CSW-110") {
    val onewayResponse = mock[OnewayResponse]
    withBehavior { case Oneway(`setup`, replyTo) =>
      replyTo ! onewayResponse
    } check { cs =>
      cs.oneway(setup).futureValue should ===(onewayResponse)
    }
  }

  test("should send oneway command and match state | CSW-110") {
    val stateName     = StateName(randomString5)
    val demandMatcher = DemandMatcher(DemandState(prefix, stateName), timeout = timeout)
    withBehavior {
      case Oneway(`setup`, replyTo)                       => replyTo ! Accepted(id)
      case ComponentStateSubscription(Subscribe(replyTo)) => replyTo ! CurrentState(prefix, stateName)
    } check { cs =>
      cs.onewayAndMatch(setup, demandMatcher).futureValue should ===(Completed(id))
    }
  }

  test("should query current state from the sequencer | CSW-110") {
    val queryResponse = mock[SubmitResponse]
    withBehavior { case Query(`id`, replyTo) =>
      replyTo ! queryResponse
    } check { cs =>
      cs.query(id).futureValue should ===(queryResponse)
    }
  }

  test("should query final response from the sequencer | CSW-110") {
    val queryFinalResponse = mock[SubmitResponse]
    withBehavior { case QueryFinal(`id`, replyTo) =>
      replyTo ! queryFinalResponse
    } check { cs =>
      cs.queryFinal(id).futureValue should ===(queryFinalResponse)
    }
  }
}
