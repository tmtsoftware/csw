package csw.command.client.internal

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.internal.MiniCRM.MiniCRMMessage.{
  AddResponse,
  AddStarted,
  GetResponses,
  GetStarters,
  GetWaiters,
  Print,
  Query,
  Query2,
  QueryFinal
}
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.{CommandNotAvailable, QueryResponse, SubmitResponse}
import csw.params.core.models.Id

import scala.collection.mutable.ListBuffer

object MiniCRM {

  type Responses            = List[SubmitResponse]
  type Starters             = List[QueryResponse]
  type Waiters              = List[(Id, ActorRef[SubmitResponse])]
  private type ResponseList = SizedList[SubmitResponse]
  private type StartedList  = SizedList[SubmitResponse]
  private type WaiterList   = SizedList[(Id, ActorRef[SubmitResponse])]

  sealed trait CRMMessage

  object MiniCRMMessage {

    case class AddResponse(commandResponse: SubmitResponse)                 extends CRMMessage
    case class AddStarted(startedResponse: SubmitResponse)                  extends CRMMessage
    case class QueryFinal(runId: Id, replyTo: ActorRef[SubmitResponse])     extends CRMMessage
    case class Query(runId: Id, replyTo: ActorRef[QueryResponse])           extends CRMMessage
    case class Query2(runId: Id, replyTo: ActorRef[Option[SubmitResponse]]) extends CRMMessage

    case object Print                                                              extends CRMMessage
    case class GetResponses(replyTo: ActorRef[List[SubmitResponse]])               extends CRMMessage
    case class GetWaiters(replyTo: ActorRef[List[(Id, ActorRef[SubmitResponse])]]) extends CRMMessage
    case class GetStarters(replTo: ActorRef[List[SubmitResponse]])                 extends CRMMessage
  }

  def make(startedSize: Int = 10, responseSize: Int = 10, waiterSize: Int = 10): Behavior[CRMMessage] =
    Behaviors.setup(_ => handle(new StartedList(startedSize), new ResponseList(responseSize), new WaiterList(waiterSize)))

  // scalastyle:off method.length
  // scalastyle:off cyclomatic.complexity
  def handle(startedList: StartedList, responseList: ResponseList, waiterList: WaiterList): Behavior[CRMMessage] = {
    //println(s"Handle: $startedList, $responseList, $waiterList")
    Behaviors.receive { (_, message) =>
      message match {
        case AddResponse(cmdResponse) =>
          // The new command state -- only way to get this is from Behavior receiving Started
          println("Add response: " + cmdResponse)
          handle(startedList, responseList.append(cmdResponse), updateWaiters(waiterList, cmdResponse))
        case AddStarted(startedResponse) =>
          handle(startedList.append(startedResponse), responseList, waiterList)
        case QueryFinal(runId, replyTo) =>
          // If there is a response, send the response to replyTo and do not save
          val augmentedWaiterList = updateWaiterList(waiterList, runId, replyTo)
          responseList.query(_.runId == runId) match {
            case Some(cr) =>
              handle(startedList, responseList, updateWaiters(augmentedWaiterList, cr))
            case None =>
              handle(startedList, responseList, augmentedWaiterList)
          }
        case Query(runId, replyTo: ActorRef[QueryResponse]) =>
          replyTo ! getResponse(startedList, responseList, runId)
          Behavior.same
        case Query2(runId, replyTo: ActorRef[Option[SubmitResponse]]) =>
          replyTo ! getResponse2(startedList, responseList, runId)
          Behavior.same
        case GetWaiters(replyTo) =>
          replyTo ! waiterList.toList
          Behavior.same
        case GetResponses(replyTo) =>
          replyTo ! responseList.toList
          Behaviors.same
        case GetStarters(replyTo) =>
          replyTo ! startedList.toList
          Behaviors.same
        case Print =>
          println("PRINT MINCRM")
          //println(s"responseList: $responseList, waiterList: $waiterList")
          Behaviors.same
        case _ =>
          Behaviors.same
      }
    }
  }
  // Waiters are always added, no matter what. There should never be more than one waiter for an Id, but
  // things work fine if there are more than 1
  def updateWaiterList(waiterList: WaiterList, runId: Id, replyTo: ActorRef[SubmitResponse]): WaiterList =
    waiterList.append((runId, replyTo))

  // First look in the waiters, if nothing there maybe look in the started list
  def getResponse(startedList: StartedList, responseList: ResponseList, runId: Id): QueryResponse =
    responseList
      .query(_.runId == runId)
      .getOrElse(
        startedList
          .query(_.runId == runId)
          .getOrElse(CommandNotAvailable(CommandName("CommandNotAvailable"), runId))
      )

  def getResponse2(startedList: StartedList, responseList: ResponseList, runId: Id): Option[SubmitResponse] =
    responseList
      .query(_.runId == runId)
      .orElse(startedList.query(_.runId == runId))

  def updateWaiters(waiterList: WaiterList, response: SubmitResponse): WaiterList = {
    // Send final response to any waiters that match runId
    waiterList.foreach(
      w =>
        if (w._1 == response.runId) {
          //println(s"Updating waiter: ${w._2} with ${w._1}")
          w._2 ! response
        }
    )
    // Remove all waiters that match runId
    waiterList.remove(_._1 == response.runId)
  }

  private class SizedList[T](max: Int, initList: List[T] = List.empty) extends Traversable[T] {
    val list: ListBuffer[T] = ListBuffer() ++= initList

    def append(sr: T): SizedList[T] = {
      if (list.size == max) {
        list.trimStart(1)
      }
      list.append(sr)
      this
    }

    override def toList: List[T] = list.toList

    def remove(f: T => Boolean): SizedList[T] = {
      val newList = list.filterNot(f)
      new SizedList(max, newList.toList)
    }

    def query(f: T => Boolean): Option[T] = list.find(f)

    def foreach[U](f: T => U): Unit = list.foreach(f)

    override def toString: String = s"SizedList(${list.toString()})"
  }
}
