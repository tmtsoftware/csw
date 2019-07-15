package csw.command.client

import akka.http.scaladsl.Http
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.ByteString
import csw.location.api.scaladsl.LocationService
import csw.location.model.scaladsl.ComponentType
import csw.params.commands.CommandResponse.{Error, OnewayResponse, SubmitResponse, ValidateResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.location.model.scaladsl.Connection.HttpConnection
import io.bullet.borer.Json

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Support for sending commands to an HTTP service (normally from a CSW component).
 *
 * @param system the typed actor system
 * @param locationService used to locate the service
 * @param connection describes the connection to the HTTP service
 */
case class HttpCommandService(
    system: akka.actor.typed.ActorSystem[Nothing],
    locationService: LocationService,
    connection: HttpConnection
) {

  import csw.params.core.formats.ParamCodecs._

  implicit val sys: akka.actor.ActorSystem = system.toUntyped
  implicit val mat: Materializer           = ActorMaterializer()(system)
  implicit val ec: ExecutionContext        = system.executionContext

  /**
   * Posts a command to the given HTTP connection and returns the response as a CommandResponse.
   * It is assumed that the HTTP service accepts the command as JSON encoded and responds with a
   * JSON encoded CommandResponse. The pycsw project provides support for creating such an HTTP server
   * in Python. Note that the HTTP service must also be registered with the Location Service.
   *
   * @param method "submit", "oneway" or "validate", corresponding to the CommandService methods.
   * @param controlCommand the command to send to the service
   * @return the command response or an Error response, if something fails
   */
  private def handleCommand(method: String, controlCommand: ControlCommand): Future[CommandResponse] = {

    def concatByteStrings(source: Source[ByteString, _]): Future[ByteString] = {
      val sink = Sink.fold[ByteString, ByteString](ByteString()) {
        case (acc, bs) =>
          acc ++ bs
      }
      source.runWith(sink)
    }

    async {
      val maybeLocation = await(locationService.find(connection))
      maybeLocation match {
        case Some(loc) =>
          val componentName = controlCommand.commandName
          val componentType = ComponentType.Service
          // For compatibility with ESW, use the following style URI
          val uri  = s"http://${loc.uri.getHost}:${loc.uri.getPort}/command/${componentType.name}/${componentName.name}/$method"
          val json = Json.encode(controlCommand).toUtf8String
          val response = await(
            Http(sys).singleRequest(
              HttpRequest(
                HttpMethods.POST,
                uri,
                entity = HttpEntity(ContentTypes.`application/json`, json)
              )
            )
          )
          if (response.status == StatusCodes.OK) {
            val bs = await(concatByteStrings(response.entity.dataBytes))
            Json.decode(bs.toArray).to[CommandResponse].value
          } else {
            Error(controlCommand.runId, s"Error response from ${connection.componentId.name}: $response")
          }
        case None =>
          Error(controlCommand.runId, s"Can't locate connection for ${connection.componentId.name}")
      }
    }
  }

  /**
   * Posts a submit command to the given HTTP connection and returns the response as a SubmitResponse.
   * It is assumed that the HTTP service accepts the command as JSON encoded and responds with a
   * JSON encoded CommandResponse.
   * Note that the HTTP service must also be registered with the Location Service.
   *
   * @param controlCommand the command to send to the service
   * @return the command response or an Error response, if something fails
   */
  def submit(controlCommand: ControlCommand): Future[SubmitResponse] =
    handleCommand("submit", controlCommand).map(_.asInstanceOf[SubmitResponse])

  /**
   * Posts a oneway command to the given HTTP connection and returns a OnewayResponse.
   * It is assumed that the HTTP service accepts the command as JSON encoded.
   * Note that the HTTP service must also be registered with the Location Service.
   *
   * @param controlCommand the command to send to the service
   * @return a future OnewayResponse: Accepted or Invalid, if there was an error
   */
  def oneway(controlCommand: ControlCommand): Future[OnewayResponse] =
    handleCommand("oneway", controlCommand).map(_.asInstanceOf[OnewayResponse])

  /**
   * Posts a validate command to the given HTTP connection and returns a ValidateResponse.
   * It is assumed that the HTTP service accepts the command as JSON encoded.
   * Note that the HTTP service must also be registered with the Location Service.
   *
   * @param controlCommand the command to send to the service
   * @return a future OnewayResponse: Accepted or Invalid, if there was an error
   */
  def validate(controlCommand: ControlCommand): Future[ValidateResponse] =
    handleCommand("validate", controlCommand).map(_.asInstanceOf[ValidateResponse])
}
