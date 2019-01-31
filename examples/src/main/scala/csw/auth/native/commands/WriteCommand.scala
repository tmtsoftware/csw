package csw.auth.native.commands

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import csw.aas.native.api.NativeAppAuthAdapter

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

// #write-command
class WriteCommand(val nativeAppAuthAdapter: NativeAppAuthAdapter, value: String)(implicit val actorSystem: ActorSystem)
    extends AppCommand {
  override def run(): Unit = {

    nativeAppAuthAdapter.getAccessTokenString() match {
      case Some(token) =>
        val bearerToken = headers.OAuth2BearerToken(token)
        val url         = s"http://localhost:7000/data?value=$value"
        val response =
          Await.result(
            Http().singleRequest(
              HttpRequest(
                method = HttpMethods.POST,
                uri = Uri(url),
                headers = List(headers.Authorization(bearerToken))
              )
            ),
            2.seconds
          )

        response.status match {
          case StatusCodes.OK           => println("Success")
          case StatusCodes.Unauthorized => println("Authentication failed")
          case StatusCodes.Forbidden    => println("Permission denied")
          case code                     => println(s"Unrecognised error: http status code = ${code.value}")
        }

      case None =>
        println("you need to login before executing this command")
        System.exit(1)
    }
  }
}
// #write-command
