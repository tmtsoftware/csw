package csw.auth.native.commands
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.ActorMaterializer
import csw.auth.native.commands.ReadCommand._

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

// #read-command
class ReadCommand(implicit val actorSystem: ActorSystem) extends AppCommand {
  override def run(): Unit = {
    val url      = "http://localhost:7000/data"
    val response = Await.result(Http().singleRequest(HttpRequest(uri = Uri(url))), 2.seconds)
    println(convertToString(response.entity))
  }
}
// #read-command

object ReadCommand {
  def convertToString(entity: ResponseEntity)(implicit actorSystem: ActorSystem): String = {
    implicit val ec: ExecutionContext   = actorSystem.dispatcher
    implicit val mat: ActorMaterializer = ActorMaterializer()
    Await.result(Unmarshaller.stringUnmarshaller(entity), 2.seconds)
  }
}
