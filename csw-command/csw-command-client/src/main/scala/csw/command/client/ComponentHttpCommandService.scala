package csw.command.client

import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl
import akka.util.{ByteString, Timeout}
import csw.location.models.HttpLocation
import csw.params.commands.CommandResponse.{Error, OnewayResponse, Started, SubmitResponse, ValidateResponse, isNegative}
import csw.params.commands.{CommandName, CommandResponse, ControlCommand}
import csw.params.core.models.Id
import io.bullet.borer.Json
import csw.params.core.formats.ParamCodecs._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Support for sending commands to an HTTP service (normally from a CSW component).
 *
 */
case class ComponentHttpCommandService(componentLocation: HttpLocation)(implicit val actorSystem: ActorSystem[_]) {

  private implicit val sys: akka.actor.ActorSystem = actorSystem.toUntyped
  private implicit val ec: ExecutionContext        = actorSystem.executionContext
  private implicit val mat: Materializer           = scaladsl.ActorMaterializer()
  private implicit val scheduler: Scheduler        = actorSystem.scheduler

  private val componentName = componentLocation.connection.componentId.name

  private def concatByteStrings(source: Source[ByteString, _]): Future[ByteString] = {
    val sink = Sink.fold[ByteString, ByteString](ByteString()) {
      case (acc, bs) =>
        acc ++ bs
    }
    source.runWith(sink)
  }

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
  private def handleCommand(method: String, controlCommand: ControlCommand): Future[CommandResponse] = async {
    // For compatibility with ESW, use the following style URI
    //val uri  = s"http://${loc.uri.getHost}:${loc.uri.getPort}/command/$componentName/$method"
    val uri = s"${componentLocation.uri.toString}/command/$method"

    println("URI: " + uri)
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
      // Server error: Return error with generated runId
      Error(controlCommand.commandName, Id(), s"Error response from ${componentName}: $response")
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
   * Submit a command and Subscribe for the result if it was successfully validated as `Started` to get a
   * final [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param controlCommand the [[csw.params.commands.ControlCommand]] payload
   * @return a CommandResponse as a Future value
   */
  def submitAndWait(controlCommand: ControlCommand)(implicit timeout: Timeout): Future[SubmitResponse] =
    submit(controlCommand).flatMap {
      case s: Started => queryFinal(s.runId)
      case x          => Future.successful(x)
    }

  /**
   * Submit multiple commands and get a List of [[csw.params.commands.CommandResponse.SubmitResponse]] for all commands. The CommandResponse can be a response
   * of validation (Accepted, Invalid) or a final Response. In case of response as `Accepted`, final CommandResponse can be obtained by using `subscribe` API.
   *
   * @param submitCommands the set of [[csw.params.commands.ControlCommand]] payloads
   * @return a Source of CommandResponse as a stream of CommandResponses for all commands
   */
  def submitAllAndWait(
      submitCommands: List[ControlCommand]
  )(implicit timeout: Timeout): Future[List[SubmitResponse]] = {
    // This exception is used to pass the failing command response to the recover to shut down the stream
    class CommandFailureException(val r: SubmitResponse) extends Exception(r.toString)

    Source(submitCommands)
      .mapAsync(1)(submitAndWait)
      .map { response =>
        if (isNegative(response))
          throw new CommandFailureException(response)
        else
          response
      }
      .recover {
        // If the command fails, then terminate but return the last response giving the problem, others are ignored
        case ex: CommandFailureException => ex.r
      }
      .toMat(Sink.seq)(Keep.right)
      .run()
      .map(_.toList)
  }

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

  /**
   * Query for the final result of a long running command which was sent as Submit to get a [[csw.params.commands.CommandResponse.SubmitResponse]] as a Future
   *
   * @param commandRunId the runId of the command for which response is required
   * @return a CommandResponse as a Future value
   */
  def queryFinal(commandRunId: Id)(implicit timeout: Timeout): Future[SubmitResponse] = async {
    assert(timeout.duration.isFinite) // FIXME: Just to get rid of the warning, for now

    // For compatibility with ESW, use the following style URI
    //val uri = s"http://${loc.uri.getHost}:${loc.uri.getPort}/command/$componentType/$componentName/${commandRunId.id}"
    val uri = componentLocation.uri.toString
    val response = await(
      Http(sys).singleRequest(
        HttpRequest(
          HttpMethods.GET,
          uri
        )
      )
    )
    if (response.status == StatusCodes.OK) {
      val bs = await(concatByteStrings(response.entity.dataBytes))
      Json.decode(bs.toArray).to[SubmitResponse].value
    } else {
      // Server error: Return error with generated runId
      Error(CommandName("ERROR"), commandRunId, s"Error response from $componentName: $response")
    }
  }

}
