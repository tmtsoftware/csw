package csw.framework.internal.supervisor

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import csw.command.client.messages.CommandMessage.{Submit, Validate}
import csw.command.client.messages.SupervisorMessage
import csw.framework.internal.supervisor.ComponentHttpBehavior.ComponentHttpMessage.Start
import csw.location.client.HttpCodecs
import csw.network.utils.Networks
import csw.params.commands.CommandResponse.{SubmitResponse, ValidateResponse}
import csw.params.commands.ControlCommand
import csw.params.core.formats.ParamCodecs

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ComponentHttpBehavior {
  implicit val timeout: Timeout = 3.seconds

  sealed trait ComponentHttpMessage
  object ComponentHttpMessage {
    case class Start(replyTo: ActorRef[Int]) extends ComponentHttpMessage
    case class Shutdown() extends ComponentHttpMessage
  }

  def make(supervisor: ActorRef[SupervisorMessage], componentName: String): Behavior[ComponentHttpMessage] = {
    Behaviors.setup { ctx =>
      implicit val scheduler: Scheduler = ctx.system.scheduler

      println("Done initHttp")
      Behaviors.receiveMessage {
        case Start(replyTo) =>
          println("Received Start")
          initHttpEndPoint(ctx, ComponentRoutes(ctx, supervisor).route(componentName)).map { sb =>
            println("POrt is: " + sb.localAddress.getPort)
            replyTo ! sb.localAddress.getPort
          }
          Behaviors.same
        case a =>
          println("Got some other message: " + a)
          Behaviors.same
      }
    }
  }

  case class ComponentRoutes(ctx: ActorContext[_], supervisor: ActorRef[SupervisorMessage]) extends ParamCodecs with HttpCodecs {
    implicit val scheduler: Scheduler = ctx.system.scheduler

    def componentRoutes(componentName: String): Route = {
      pathPrefix(componentName) {
        pathPrefix("command") {
          post {
            path("validate") {
              entity(as[ControlCommand]) { command =>
                println("Validate: " + command)
                val response: Future[ValidateResponse] = supervisor ? (Validate(command, _))
                complete(response)
              }
            } ~
            path("submit") {
              entity(as[ControlCommand]) { command =>
                println("Command: " + command)
                val response: Future[SubmitResponse] = supervisor ? (Submit(command, _))
                complete(response)
              }
            }
          }
        }
      }
    }
    def route(componentName: String): Route = componentRoutes(componentName)
  }

  def initHttpEndPoint(ctx: ActorContext[_], route: Route): Future[ServerBinding] = async {
    implicit val actorSystem  = ctx.system.toUntyped
    implicit val materializer = akka.stream.ActorMaterializer()

    val hostname = Networks().hostname
    println("Hostname: " + hostname)
    val serverBinding = await(Http().bindAndHandle(handler = route, interface = hostname, port = 0))
    println("server: " + serverBinding)

    serverBinding
  }

}

/*
  serverBinding.onComplete {
  case Success(binding) =>
  println("binding: " + binding)
  //        registerHttpWithLocationService(binding.localAddress.getPort)
  println("Done here")
  case Failure(throwable) => println("There is an exception: " + throwable)
  }

 */
