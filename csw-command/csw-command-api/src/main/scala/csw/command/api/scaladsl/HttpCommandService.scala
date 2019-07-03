package csw.command.api.scaladsl

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.ByteString
import csw.location.api.models.Connection.HttpConnection
import csw.params.commands.CommandResponse.{Error, SubmitResponse}
import csw.params.commands.Setup
import io.bullet.borer.Cbor

import scala.async.Async._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import csw.location.api.scaladsl.LocationService

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

  /**
   * Posts a Setup command to the given HTTP connection and returns the response as a SubmitResponse.
   * It is assumed that the HTTP service accepts the Setup as CBOR encoded and responds with a
   * CBOR encoded SubmitResponse. The pycsw project will provide support for creating such an HTTP server
   * in Python. Note that the HTTP service must also be registered with the Location Service.
   *
   * @param setup the Setup command to send to the service
   * @return the Submit response or an Error response, if something fails
   */
  def submit(setup: Setup): Future[SubmitResponse] = {
    import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
    import csw.params.core.formats.ParamCodecs._
    implicit val sys: akka.actor.ActorSystem = system.toUntyped
    implicit val mat: Materializer           = ActorMaterializer()(system)
    implicit val ec: ExecutionContext        = system.executionContext

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
          val uri = s"http://${loc.uri.getHost}:${loc.uri.getPort}/submit"
          val response = await(
            Http(sys).singleRequest(
              HttpRequest(
                HttpMethods.POST,
                uri,
                entity = HttpEntity(ContentTypes.`application/octet-stream`, Cbor.encode(setup).toByteArray)
              )
            )
          )
          if (response.status == StatusCodes.OK) {
            val bs = await(concatByteStrings(response.entity.dataBytes))
            Cbor.decode(bs.toArray).to[SubmitResponse].value
          } else {
            Error(setup.runId, s"Error response from ${connection.componentId.name}: $response")
          }
        case None =>
          Error(setup.runId, s"Can't locate connection for ${connection.componentId.name}")
      }
    }
  }
}
