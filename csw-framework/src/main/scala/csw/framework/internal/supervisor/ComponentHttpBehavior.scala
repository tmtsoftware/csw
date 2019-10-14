package csw.framework.internal.supervisor

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import csw.command.client.messages.CommandMessage.{Oneway, Submit, Validate}
import csw.command.client.messages.{Query, QueryFinal, SupervisorMessage}
import csw.framework.internal.supervisor.ComponentHttpBehavior.ComponentHttpMessage.Start
import csw.location.client.HttpCodecs
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.Networks
import csw.params.commands.CommandResponse.{OnewayResponse, QueryResponse, SubmitResponse, ValidateResponse}
import csw.params.commands.ControlCommand
import csw.params.core.formats.ParamCodecs
import csw.params.core.models.Id

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ComponentHttpBehavior {
  implicit val defaultTimeout: Timeout = 10.seconds

  sealed trait ComponentHttpMessage

  object ComponentHttpMessage {

    case class Start(replyTo: ActorRef[Int]) extends ComponentHttpMessage
    case class Shutdown()                    extends ComponentHttpMessage

  }

  def make(
      loggerFactory: LoggerFactory,
      supervisor: ActorRef[SupervisorMessage],
      componentName: String
  ): Behavior[ComponentHttpMessage] = Behaviors.setup { ctx =>
    val log: Logger = loggerFactory.getLogger(ctx)

    def receive(): Behavior[ComponentHttpMessage] = {
      Behaviors.receiveMessage {
        case Start(replyTo) =>
          log.info(s"Setting up HTTP endpoint for component: $componentName")
          initHttpEndPoint(ctx, ComponentRoutes(ctx, supervisor).route(componentName)).map { sb =>
            replyTo ! sb.localAddress.getPort
          }
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }

    case class ComponentRoutes(ctx: ActorContext[_], supervisor: ActorRef[SupervisorMessage])
        extends ParamCodecs
        with HttpCodecs {
      implicit val scheduler: Scheduler = ctx.system.scheduler

      def componentRoutes(componentName: String): Route = {
        pathPrefix(componentName) {
          post {
            path("validate") {
              entity(as[ControlCommand]) { command =>
                val response: Future[ValidateResponse] = supervisor ? (Validate(command, _))
                complete(response)
              }
            } ~
            path("submit") {
              entity(as[ControlCommand]) { command =>
                val response: Future[SubmitResponse] = supervisor ? (Submit(command, _))
                complete(response)
              }
            } ~
            path("oneway") {
              entity(as[ControlCommand]) { command =>
                val response: Future[OnewayResponse] = supervisor ? (Oneway(command, _))
                complete(response)
              }
            }
          } ~
          get {
            pathPrefix("query" / Segment) { runId =>
              val response: Future[QueryResponse] = supervisor ? (Query(Id(runId), _))
              complete(response)
            } ~
            pathPrefix("queryFinal" / Segment) { runId =>
              val response: Future[QueryResponse] = supervisor ? (QueryFinal(Id(runId), _))
              complete(response)
            }
          }
        }
      }

      def route(componentName: String): Route = componentRoutes(componentName)
    }

    def initHttpEndPoint(ctx: ActorContext[_], route: Route): Future[ServerBinding] = async {
      implicit val actorSystem: ActorSystem        = ctx.system.toUntyped
      implicit val materializer: ActorMaterializer = akka.stream.ActorMaterializer()

      val hostname = Networks().hostname
      // The following uses the hostname and creates a port for the HTTP endpoint
      val serverBinding = await(Http().bindAndHandle(handler = route, interface = hostname, port = 0))
      log.info(s"HTTP endpoint for component: $componentName at: $serverBinding")

      serverBinding
    }

    // Start
    receive()
  }

}
