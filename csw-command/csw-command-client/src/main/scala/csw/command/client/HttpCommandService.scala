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
import csw.params.commands.CommandIssue.{OtherIssue, UnresolvedLocationsIssue}
import csw.params.commands.CommandResponse.{Accepted, Error, Invalid, OnewayResponse, SubmitResponse}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.location.model.scaladsl.Connection.HttpConnection
import csw.params.core.formats.JsonSupport._
import play.api.libs.json.Json

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

  implicit val sys: akka.actor.ActorSystem = system.toUntyped
  implicit val mat: Materializer           = ActorMaterializer()(system)
  implicit val ec: ExecutionContext        = system.executionContext

  /**
   * Posts a command to the given HTTP connection and returns the response as a CommandResponse.
   * It is assumed that the HTTP service accepts the command as JSON encoded and responds with a
   * JSON encoded CommandResponse. The pycsw project provides support for creating such an HTTP server
   * in Python. Note that the HTTP service must also be registered with the Location Service.
   *
   * @param controlCommand the command to send to the service
   * @return the command response or an Error response, if something fails
   */
  def submit(controlCommand: ControlCommand): Future[SubmitResponse] = {

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
          val uri = s"http://${loc.uri.getHost}:${loc.uri.getPort}/command/${componentType.name}/${componentName.name}/submit"
//          val json = Json.encode(controlCommand).toUtf8String
          val json = Json.toJson(controlCommand).toString()
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
//            Json.decode(bs.toArray).to[SubmitResponse].value
            Json.parse(bs.utf8String).as[CommandResponse].asInstanceOf[SubmitResponse]
          } else {
            Error(controlCommand.runId, s"Error response from ${connection.componentId.name}: $response")
          }
        case None =>
          Error(controlCommand.runId, s"Can't locate connection for ${connection.componentId.name}")
      }
    }
  }

  /**
   * Posts a command to the given HTTP connection without expecting a response.
   * It is assumed that the HTTP service accepts the command as JSON encoded.
   * The pycsw project will provide support for creating such an HTTP server in Python.
   * Note that the HTTP service must also be registered with the Location Service.
   *
   * @param controlCommand the command to send to the service
   * @return a future OnewayResponse, Accepted or Invalid, if there was an error
   */
  def oneway(controlCommand: ControlCommand): Future[OnewayResponse] = {
    async {
      val maybeLocation = await(locationService.find(connection))
      maybeLocation match {
        case Some(loc) =>
          val componentName = controlCommand.commandName
          val componentType = ComponentType.Service
          // For compatibility with ESW, use the following style URI
          val uri  = s"http://${loc.uri.getHost}:${loc.uri.getPort}/command/${componentType.name}/${componentName.name}/oneway"
          val json = Json.toJson(controlCommand).toString()
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
            Accepted(controlCommand.runId)
          } else {
            Invalid(controlCommand.runId, OtherIssue(s"Error response from ${connection.componentId.name}: $response"))
          }
        case None =>
          Invalid(controlCommand.runId, UnresolvedLocationsIssue(s"Can't locate connection for ${connection.componentId.name}"))
      }
    }
  }
}
