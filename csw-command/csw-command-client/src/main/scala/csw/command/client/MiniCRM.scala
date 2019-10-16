package csw.command.client

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.github.benmanes.caffeine.cache.{Cache, Caffeine, RemovalCause}
import csw.params.commands.{CommandName, CommandResponse}
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, Started, SubmitResponse}
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
object MiniCRM {

  case class CRMState(response: SubmitResponse, subscribers: Set[ActorRef[QueryResponse]] = Set.empty)

  sealed trait CRMMessage
  object CRMMessage {
    case class AddResponse(commandResponse: SubmitResponse)            extends CRMMessage
    case class QueryFinal(runId: Id, replyTo: ActorRef[QueryResponse]) extends CRMMessage
    case class Query(runId: Id, replyTo: ActorRef[QueryResponse])      extends CRMMessage

    private[command] case class GetState(replyTo: ActorRef[Map[Id, CRMState]]) extends CRMMessage
  }
  import CRMMessage._

  //noinspection ScalaStyle
  def make(maxSize: Int = 10): Behavior[CRMMessage] =
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
          case AddResponse(cmdResponse) => cache.put(cmdResponse.runId, CRMState(cmdResponse))
          case QueryFinal(runId, replyTo) =>
            val currentStateMaybe = Option(cache.getIfPresent(runId))
            currentStateMaybe match {
              case Some(state @ CRMState(_: Started, _)) =>
                cache.put(runId, state.copy(subscribers = state.subscribers + replyTo))
              case Some(CRMState(finalResponse, _)) => replyTo ! finalResponse
              case None                             => replyTo ! CommandNotAvailable(CommandName("CommandNotAvailable"), runId)
            }
          case Query(runId, replyTo: ActorRef[QueryResponse]) =>
            val currentStateMaybe = Option(cache.getIfPresent(runId))
            currentStateMaybe match {
              case Some(CRMState(response, _)) => replyTo ! response
              case None                        => replyTo ! CommandNotAvailable(CommandName("CommandNotAvailable"), runId)
            }
          case GetState(replyTo) => replyTo ! cache.asMap().asScala.toMap
          case _                 =>
        }
        Behaviors.same
      }
    }
}
