package csw.command.client.internal

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, Started, SubmitResponse}
import csw.params.core.models.Id

import scala.collection.mutable.ListBuffer

/**
 * miniCRM is described here.
 * miniCRM is designed to be used with a CommandService object, which encapsulates commands sent to one
 * Assembly or HCD. A miniCRM supports the two CommandService methods called query and queryFinal.
 * It keeps three list types: StartedList, ResponseList, and WaiterList
 * StartedList: This is a list of commands that have returned Started to the CommandService. They are still
 *               executing, having return Started
 * ResponseList: This is a list of SubmitResponses published by the component this CommandService commands. This is
 *               called from the PubSub handler provided by the CommandService when subscribing to command updates
 * WaiterList: This is a list of queryFinal callers.  The list contains tuples of (id, ActorRef[SubmitResponse). Each
 *              entry is an actor waiting for the final response for the command with the given id.
 *
 * miniCRM is written in the "immutable" style so all state is passed between calls to new Behaviors
 *
 * The CommandService only submits Started commands to the miniCRM. When a Started SubmitResponse is received,
 * miniCRM receives an AddStarted method. Whenever the component associated with the
 */
object MiniCRM {

  type Responses            = List[SubmitResponse]
  type Starters             = List[QueryResponse]
  type Waiters              = List[(Id, ActorRef[SubmitResponse])]
  private type ResponseList = SizedList[SubmitResponse]
  private type StartedList  = SizedList[SubmitResponse]
  private type WaiterList   = SizedList[(Id, ActorRef[SubmitResponse])]

  sealed trait CRMMessage
  object MiniCRMMessage {
    case class AddResponse(commandResponse: SubmitResponse)             extends CRMMessage
    case class AddStarted(startedResponse: Started)                     extends CRMMessage
    case class QueryFinal(runId: Id, replyTo: ActorRef[SubmitResponse]) extends CRMMessage
    case class Query(runId: Id, replyTo: ActorRef[QueryResponse])       extends CRMMessage

    case class Print(replyTo: ActorRef[String])                                    extends CRMMessage
    case class GetResponses(replyTo: ActorRef[List[SubmitResponse]])               extends CRMMessage
    case class GetWaiters(replyTo: ActorRef[List[(Id, ActorRef[SubmitResponse])]]) extends CRMMessage
    case class GetStarters(replTo: ActorRef[List[SubmitResponse]])                 extends CRMMessage
  }
  import MiniCRMMessage._

  //noinspection ScalaStyle
  def make(startedSize: Int = 10, responseSize: Int = 10, waiterSize: Int = 10): Behavior[CRMMessage] =
    Behaviors.setup(_ => handle(new StartedList(startedSize), new ResponseList(responseSize), new WaiterList(waiterSize)))

  // scalastyle:off method.length
  // scalastyle:off cyclomatic.complexity
  private def handle(startedList: StartedList, responseList: ResponseList, waiterList: WaiterList): Behavior[CRMMessage] =
    Behaviors.receive { (_, message) =>
      message match {
        case AddResponse(cmdResponse) =>
          // Called when a StartedCommand publishes a "final" SubmitResponse for a long-running command
          handle(startedList, responseList.append(cmdResponse), updateWaiters(waiterList, cmdResponse))
        case AddStarted(startedResponse) =>
          // The new command state -- only way to get this is from Behavior receiving Started CommandResponse
          // This is called by submit and submitAndWait if the destination component returns Started
          handle(startedList.append(startedResponse), responseList, waiterList)
        case QueryFinal(runId, replyTo) =>
          // If there is a response, send the response to replyTo and do not save,
          val augmentedWaiterList = updateWaiterList(waiterList, runId, replyTo)
          responseList.query(_.runId == runId) match {
            case Some(cr) =>
              // Note that we have a response, so updateWaiters, will send the response and remove from WaiterList
              handle(startedList, responseList, updateWaiters(augmentedWaiterList, cr))
            case None =>
              // Just add the new waiter to the WaiterList
              handle(startedList, responseList, augmentedWaiterList)
          }
        case Query(runId, replyTo: ActorRef[QueryResponse]) =>
          // check for a response or started and return the first match
          replyTo ! getResponse(startedList, responseList, runId)
          Behavior.same
        case GetWaiters(replyTo) =>
          // Used only for tests
          replyTo ! waiterList.toList
          Behavior.same
        case GetResponses(replyTo) =>
          // Used only for tests
          replyTo ! responseList.toList
          Behaviors.same
        case GetStarters(replyTo) =>
          // Used only for tests
          replyTo ! startedList.toList
          Behaviors.same
        case Print(replyTo) =>
          // Used only for tests
          replyTo ! s"responseList: $responseList, waiterList: $waiterList"
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }

  /**
   * Update a WaiterList and return the new WaiterList. This is called from more than one place so it's
   * in its own function.
   * Waiters are always added, no matter what. There should never be more than one waiter for an Id, but
   * things work fine if there are more than 1
   * @param waiterList a WaiterList of Id, ActurRef[SubmitResponse] tuples.
   * @param runId the new runId
   * @param replyTo the new ActorRef[SubmitResponse]
   * @return the updated WaiterList
   */
  private def updateWaiterList(waiterList: WaiterList, runId: Id, replyTo: ActorRef[SubmitResponse]): WaiterList =
    waiterList.append((runId, replyTo))

  /**
   * getResponse function handles the query message. It is passed the current StartedList and ResponseList.
   * First it looks through the ResponseList to see if any SubmitResponses have been received for this runId.
   * ResponseList only has updates from started commands. If it finds it in the responseList, that means that the
   * final response has been received from the component. If it doesn't find it in the responseList, it looks in the
   * StartedList, which contains commands that have Started, but are still executing. When the input Id is found, the
   * SubmitResponse is found. If the runId is not in either list, CommandNotAvailable is returned -- QueryResponse.
   * @param startedList contains Started responses for Ids that have started, but not finished
   * @param responseList contains SubmitResponses that have been received from the destination component
   * @param runId the runId the query is interested in
   * @return a QueryResponse that matches the Id or CommandNotAvailable
   */
  private def getResponse(startedList: StartedList, responseList: ResponseList, runId: Id): QueryResponse =
    responseList
      .query(_.runId == runId)
      .getOrElse(
        startedList
          .query(_.runId == runId)
          .getOrElse(CommandNotAvailable(CommandName("CommandNotAvailable"), runId))
      )

  /**
   * updateWaiters looks through the waiterList input for any ActorRefs waiting for the id of the input response.
   * For every waiter waiting for the id of the response, the response is sent to the ActorRef. Then
   * that entry is removed from the WaiterList and returned to the caller
   * @param waiterList the list of (id, ActorRef[SubmitResponse]) -- callers of queryFinal
   * @param response a received update SubmitResponse from a component
   * @return new WaiterList, maybe be the same if no matches, or may be smaller but the number of waiters
   */
  private def updateWaiters(waiterList: WaiterList, response: SubmitResponse): WaiterList = {
    // Send final response to any waiters that match runId
    waiterList.foreach(
      w =>
        if (w._1 == response.runId) {
          w._2 ! response
        }
    )
    // Remove all waiters that match runId and return
    waiterList.remove(_._1 == response.runId)
  }

  /**
   * This is a specialized list that will only keep a maximum number of elements
   * @param max size of list to retain
   * @param initList the list can be initialized with some values
   * @tparam T the type of elements in the list
   */
  private class SizedList[T](max: Int, initList: List[T] = List.empty) {
    val list: ListBuffer[T] = ListBuffer() ++= initList

    def append(sr: T): SizedList[T] = {
      // If the list is at the maximum, remove 1 and add the new one
      if (list.size == max) {
        list.trimStart(1)
      }
      list.append(sr)
      this
    }

    def toList: List[T] = list.toList

    def remove(f: T => Boolean): SizedList[T] = {
      val newList = list.filterNot(f)
      new SizedList(max, newList.toList)
    }

    // Runs the predicate argument against each member of the list and returns first
    def query(f: T => Boolean): Option[T] = list.find(f)

    // Runs the function for every member of the list
    def foreach[U](f: T => U): Unit = list.foreach(f)

    override def toString: String = s"SizedList(${list.toString()})"
  }
}
