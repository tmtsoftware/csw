package csw.command.client

import akka.actor
import akka.actor.typed
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import csw.location.client.HttpCodecs
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType, HttpRegistration}
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.location.server.http.HTTPLocationService
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Setup}
import csw.params.core.formats.ParamCodecs
import csw.params.core.generics.KeyType.CoordKey
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Coords.EqFrame.FK5
import csw.params.core.models.Coords.SolarSystemObject.Venus
import csw.params.core.models._
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

//noinspection ScalaStyle
class HttpCommandServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with HTTPLocationService {

  implicit val typedSystem: typed.ActorSystem[_] = ActorSystem(Behaviors.empty, "HttpCommandServiceTest")
  implicit val untypedSystem: actor.ActorSystem  = typedSystem.toClassic
  implicit val ec: ExecutionContext              = typedSystem.executionContext
  implicit val mat: Materializer                 = Materializer(typedSystem)
  implicit val timeout: Timeout                  = Timeout(5.seconds)

  private val locationService = HttpLocationServiceFactory.makeLocalClient
  private val testCompName    = "testComponent"
  private val connection      = HttpConnection(ComponentId(testCompName, ComponentType.Service))

  private val basePosKey = CoordKey.make("BasePosition")
  private val prefix     = Prefix("csw.command")
  private val key1: Key[Float] =
    KeyType.FloatKey.make("assemblyEventValue1")
  private val key1b: Key[Float] =
    KeyType.FloatKey.make("assemblyEventValue1b")
  private val key2b: Key[Struct] =
    KeyType.StructKey.make("assemblyEventStructValue2b")
  private val key3: Key[Int] =
    KeyType.IntKey.make("assemblyEventStructValue3")
  private val key4: Key[Byte] =
    KeyType.ByteKey.make("assemblyEventStructValue4")

  private val testCommand = makeTestCommand()

  private def validateCommand(componentName: String, command: ControlCommand): ValidateResponse = {
    if (componentName == testCompName && command == testCommand)
      Accepted(command.runId)
    else if (componentName != testCompName)
      Invalid(command.runId, OtherIssue(s"Expected component name: $testCompName, but got $componentName"))
    else
      Invalid(command.runId, OtherIssue(s"Expected command to be: $testCommand, but got $command"))
  }

  // Test HTTP route
  private class CommandRoutes() extends ParamCodecs with HttpCodecs {

    def commandRoutes(componentType: String): Route =
      pathPrefix("command") {
        pathPrefix(componentType / Segment) { componentName =>
          post {
            path("validate") {
              entity(as[ControlCommand]) { command =>
                complete(Future.successful(validateCommand(componentName, command)))
              }
            } ~
            path("submit") {
              entity(as[ControlCommand]) { command =>
                val v = validateCommand(componentName, command)
                val x: SubmitResponse = v match {
                  case _: Accepted => Completed(command.runId)
                  case _           => Error(command.runId, "Wrong command or component name")
                }
                complete(Future.successful(x))
              }
            } ~
            path("oneway") {
              entity(as[ControlCommand]) { command =>
                complete(Future.successful(validateCommand(componentName, command)))
              }
            }
          } ~
          get {
            path(Segment) { runId =>
              val x: Future[SubmitResponse] = Future.successful(Completed(Id(runId)))
              complete(x)
            }
          }
        }
      }

    val route: Route = commandRoutes(ComponentType.Service.name)
  }

  // Creates a test command
  private def makeTestCommand(): ControlCommand = {
    import Angle._
    import Coords._

    val pm = ProperMotion(0.5, 2.33)
    val eqCoord = EqCoord(
      ra = "12:13:14.15",
      dec = "-30:31:32.3",
      frame = FK5,
      pmx = pm.pmx,
      pmy = pm.pmy
    )
    val solarSystemCoord = SolarSystemCoord(Tag("BASE"), Venus)
    val minorPlanetCoord = MinorPlanetCoord(
      Tag("GUIDER1"),
      2000,
      90.degree,
      2.degree,
      100.degree,
      1.4,
      0.234,
      220.degree
    )
    val cometCoord = CometCoord(
      Tag("BASE"),
      2000.0,
      90.degree,
      2.degree,
      100.degree,
      1.4,
      0.234
    )
    val altAzCoord = AltAzCoord(Tag("BASE"), 301.degree, 42.5.degree)
    val posParam = basePosKey.set(
      eqCoord,
      solarSystemCoord,
      minorPlanetCoord,
      cometCoord,
      altAzCoord
    )

    Setup(prefix, CommandName("testCommand"), None)
      .add(posParam)
      .add(key1b.set(1.0f, 2.0f, 3.0f))
      .add(
        key2b.set(
          Struct()
            .add(key1.set(1.0f))
            .add(key3.set(1, 2, 3)),
          Struct()
            .add(key1.set(2.0f))
            .add(key3.set(4, 5, 6))
            .add(key4.set(9.toByte, 10.toByte))
        )
      )
  }

  private val server = Http()
    .bindAndHandle(
      handler = new CommandRoutes().route,
      interface = "0.0.0.0",
      port = 0
    )
    .await

  override def beforeAll(): Unit = {
    super.beforeAll()

    locationService.register(HttpRegistration(connection, server.localAddress.getPort, "")).await
  }

  override def afterAll(): Unit = {
    super.afterAll()
    server.terminate(timeout.duration)
    typedSystem.terminate()
  }

  test("Should be able to validate, send commands, query status of commands") {
    val service = HttpCommandService(typedSystem, locationService, connection)
    assert(service.validate(testCommand).await == Accepted(testCommand.runId))
    assert(service.oneway(testCommand).await == Accepted(testCommand.runId))
    assert(service.submit(testCommand).await == Completed(testCommand.runId))
    assert(service.queryFinal(testCommand.runId).await == Completed(testCommand.runId))
  }
}
