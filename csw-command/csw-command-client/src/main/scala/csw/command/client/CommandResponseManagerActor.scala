package csw.command.client

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.github.benmanes.caffeine.cache.{Cache, Caffeine, RemovalCause}
import csw.command.client.CommandResponseManagerActor.CRMMessage._
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.commands.{CommandName, CommandResponse}
import csw.params.core.models.Id

import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

/**
 * miniCRM is designed to be a limited commandResponseManager, which encapsulates commands sent to one
 * Assembly or HCD. A miniCRM supports the two CommandService methods called query and queryFinal.
 * miniCRM is written in the "immutable" style so all state is passed between calls to new Behaviors
 *
 * The ComponentBehavior only submits Started commands to the miniCRM when a Started is returned from a submit
 * handler. When a Started SubmitResponse is received, miniCRM receives an AddStarted call from ComponentBehavior.
 * Whenever the component publishes the final completion message, it calls AddResponse, which triggers calls to any
 * waiters associated with the runId.
 *
 * Query and QueryFinal are used by the CommandService to provide the status of a command.
 * Both of these methods look within the three lists above for their responses.
 * Note that miniCRM does not handle any other commands besides ones that return Started.
 */
object CommandResponseManagerActor {

  //noinspection ScalaStyle
  def make(maxSize: Int): Behavior[CRMMessage] =
    Behaviors.setup { _ =>
      val cache: Cache[Id, CRMState] = Caffeine
        .newBuilder()
        .maximumSize(maxSize)
        .removalListener((k: Id, v: CRMState, _: RemovalCause) => {
          //todo: log a warning
          v.subscribers.foreach(_ ! CommandResponse.Error(v.response.commandName, k, "too many commands"))
        })
        .build()

      Behaviors.receiveMessage { msg =>
        msg match {
          case AddResponse(response) => cache.put(response.runId, CRMState(response))

          case UpdateResponse(newResponse) =>
            Option(cache.getIfPresent(newResponse.runId)) match {
              case None => // todo: log a warning
              case Some(state @ CRMState(response, sub))
                  if (CommandResponse.isIntermediate(response)) && (CommandResponse.isFinal(newResponse)) =>
                //notify all subscribers
                sub.foreach(_.tell(newResponse))
                // removing all subscribers
                cache.put(response.runId, state.copy(subscribers = Set.empty))
              case Some(CRMState(prevResponse, _)) if (CommandResponse.isFinal(prevResponse)) =>
              //todo: log warning
            }

          case QueryFinal(runId, replyTo) =>
            val currentStateMaybe = Option(cache.getIfPresent(runId))
            currentStateMaybe match {
              case Some(state @ CRMState(response, _)) if (CommandResponse.isIntermediate(response)) =>
                cache.put(runId, state.copy(subscribers = state.subscribers + replyTo))
              case Some(CRMState(finalResponse, _)) =>
                replyTo ! finalResponse
              case None => replyTo ! CommandNotAvailable(CommandName("CommandNotAvailable"), runId)
            }

          case Query(runId, replyTo) =>
            Option(cache.getIfPresent(runId)) match {
              case Some(CRMState(response, _)) => replyTo ! response
              case None                        => replyTo ! CommandNotAvailable(CommandName("CommandNotAvailable"), runId)
            }
          case GetState(replyTo) => replyTo ! cache.asMap().asScala.toMap
        }
        Behaviors.same
      }
    }

  sealed trait CRMMessage

  case class CRMState(response: SubmitResponse, subscribers: Set[ActorRef[QueryResponse]] = Set.empty)

  object CRMMessage {
    case class AddResponse(commandResponse: SubmitResponse)            extends CRMMessage
    case class UpdateResponse(commandResponse: SubmitResponse)         extends CRMMessage
    case class QueryFinal(runId: Id, replyTo: ActorRef[QueryResponse]) extends CRMMessage
    case class Query(runId: Id, replyTo: ActorRef[QueryResponse])      extends CRMMessage

    private[command] case class GetState(replyTo: ActorRef[Map[Id, CRMState]]) extends CRMMessage
  }
}
