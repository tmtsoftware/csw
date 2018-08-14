package csw.framework.integration

import akka.{NotUsed, actor}
import akka.actor.{ActorSystem, typed}
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.testkit.typed.TestKitSettings
import akka.stream.scaladsl.{Flow, Source}

import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.messages.commands.CommandResponse.{Completed, SubmitResponse}
import csw.messages.commands.{CommandName, ControlCommand, Setup}
import csw.messages.location.ComponentId
import csw.messages.location.ComponentType.HCD
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.models.{Id, ObsId, Prefix}
import csw.services.command.scaladsl.CommandService
import io.lettuce.core.RedisClient
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationLong



class CommandServiceTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {


    private val testWiring = new FrameworkTestWiring()
    import testWiring._

    override protected def afterAll(): Unit = shutdown()

    val prefix = Prefix("wfos.blue.filter")
    val obsId  = ObsId("Obs001")

  implicit val timeout: Timeout = 5.seconds


    val moveCmd                   = CommandName("move")
    val initCmd                   = CommandName("init")

    /*
      val hcdLocF =
        locationService.resolve(
          AkkaConnection(ComponentId("Test_Component_Running_Long_Command", ComponentType.HCD)),
          5.seconds
        )
      */
    //val hcdLocation: AkkaLocation = Await.result(hcdLocF, 10.seconds).get
    //val hcdComponent              = new CommandService(hcdLocation)

    val setupAssembly1 = Setup(prefix, moveCmd, Some(obsId))
    val setupAssembly2 = Setup(prefix, initCmd, Some(obsId))
    //val setupAssembly3 = Setup(prefix, invalidCmd, Some(obsId))
    //val setupHcd1      = Setup(prefix, shortRunning, Some(obsId))
    //val setupHcd2      = Setup(prefix, mediumRunning, Some(obsId))
    //val setupHcd3      = Setup(prefix, failureAfterValidationCmd, Some(obsId))

    def submit(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] = {
      println(s"Command: $controlCommand")
      Future(Completed(Id()))
    }

    def submitAll(submitCommands: List[ControlCommand])(implicit timeout: Timeout): SubmitResponse = {
      def g(sub: ControlCommand):Future[SubmitResponse] = submit(sub)

      //val ff = Flow[ControlCommand, SubmitResponse, NotUsed]
      val yy:Future[SubmitResponse] = Source.fromFuture(submit(submitCommands.head))
      val x = Source.fromIterator(() => submitCommands.iterator).map(f => submit(f))
      val src:Source[ControlCommand, NotUsed] = Source(submitCommands)
      //val x = src.(map(f => submit(f)))
      x.runForeach(s => println(s))

      Completed(Id())
    }

    test("test submit all 1") {
      // start component in standalone mode
      //val wiring: FrameworkWiring = FrameworkWiring.make(testActorSystem, mock[RedisClient])
      //Standalone.spawn(ConfigFactory.load("standalone.conf"), wiring)

      //val akkaConnection                = AkkaConnection(ComponentId("IFS_Detector", HCD))

      // verify component gets registered with location service
      //val eventualLocation = seedLocationService.resolve(akkaConnection, 5.seconds)
      //val maybeLocation    = Await.result(eventualLocation, 5.seconds)

      //maybeLocation.isDefined shouldBe true
      //val resolvedAkkaLocation = maybeLocation.get
      //resolvedAkkaLocation.connection shouldBe akkaConnection
      //val x = submitAll()

     // val mcs:CommandService = mock[CommandService]
      //when(mcs.submit(_)).thenReturn(Future(Completed(Id())))

      //val cs = new CommandService(resolvedAkkaLocation)
      val x = submitAll(List(setupAssembly1, setupAssembly2))

    }



}
