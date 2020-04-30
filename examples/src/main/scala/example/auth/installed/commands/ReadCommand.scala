package example.auth.installed.commands

import akka.actor.typed
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import example.auth.installed.commands.ReadCommand.convertToString

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

// #read-command
class ReadCommand(implicit val actorSystem: typed.ActorSystem[_]) extends AppCommand {
  override def run(): Unit = {
    val url      = "http://localhost:7000/data"
    val response = Await.result(Http().singleRequest(HttpRequest(uri = Uri(url))), 2.seconds)
    println(convertToString(response.entity))
  }
}
// #read-command

object ReadCommand {
  def convertToString(entity: ResponseEntity)(implicit actorSystem: typed.ActorSystem[_]): String = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    Await.result(Unmarshaller.stringUnmarshaller(entity), 2.seconds)
  }
}
