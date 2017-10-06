package csw.trombone.assembly.actors

import akka.actor.Scheduler
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.AskPattern._
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages._
import csw.messages.ccs.ValidationIssue.{
  RequiredHCDUnavailableIssue,
  UnsupportedCommandInStateIssue,
  WrongInternalStateIssue
}
import csw.messages.ccs.commands.Setup
import csw.trombone.assembly.FollowActorMessages.{SetZenithAngle, StopFollowing}
import csw.trombone.assembly.TromboneCommandHandlerMsgs._
import csw.trombone.assembly._
import csw.trombone.assembly.actors.TromboneCommandHandler.Mode
import csw.trombone.assembly.commands._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object TromboneCommandHandler {

  def make(
      assemblyContext: AssemblyContext,
      tromboneHCDIn: Option[Running],
      allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
  ): Behavior[NotFollowingMsgs] =
    Actor
      .mutable[TromboneCommandHandlerMsgs](
        ctx ⇒ new TromboneCommandHandler(ctx, assemblyContext, tromboneHCDIn, allEventPublisher)
      )
      .narrow

  sealed trait Mode
  object Mode {
    case object NotFollowing extends Mode
    case object Following    extends Mode
    case object Executing    extends Mode
  }
}

class TromboneCommandHandler(
    ctx: ActorContext[TromboneCommandHandlerMsgs],
    ac: AssemblyContext,
    tromboneHCDIn: Option[Running],
    allEventPublisher: Option[ActorRef[TrombonePublisherMsg]]
) extends MutableBehavior[TromboneCommandHandlerMsgs] {

  implicit val scheduler: Scheduler = ctx.system.scheduler
  import ctx.executionContext

  var mode: Mode = Mode.NotFollowing

  import TromboneCommandHandler._
  import TromboneStateActor._
  import ac._
  implicit val system: ActorSystem[Nothing] = ctx.system
  implicit val timeout                      = Timeout(5.seconds)

  private val tromboneStateAdapter: ActorRef[TromboneState] = ctx.spawnAdapter(TromboneStateE)

  ctx.system.eventStream.subscribe(tromboneStateAdapter, classOf[TromboneState])

  private val tromboneStateActor          = ctx.spawnAnonymous(TromboneStateActor.make())
  private var currentState: TromboneState = defaultTromboneState

  private val badHCDReference = ctx.system.deadLetters
  private val pubSubRef       = ctx.system.deadLetters
  private val tromboneHCD     = tromboneHCDIn.getOrElse(Running(badHCDReference))

  private var setElevationItem = naElevation(calculationConfig.defaultInitialElevation)

  private var followCommandActor: ActorRef[FollowCommandMessages] = _
  private var currentCommand: TromboneAssemblyCommand             = _

  private def isHCDAvailable: Boolean = tromboneHCD.componentRef != badHCDReference

  override def onMessage(msg: TromboneCommandHandlerMsgs): Behavior[TromboneCommandHandlerMsgs] = {
    (mode, msg) match {
      case (Mode.NotFollowing, x: NotFollowingMsgs) ⇒ onNotFollowing(x)
      case (Mode.Following, x: FollowingMsgs)       ⇒ onFollowing(x)
      case (Mode.Executing, x: ExecutingMsgs)       ⇒ onExecuting(x)
      case _                                        ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  def onNotFollowing(msg: NotFollowingMsgs): Unit = msg match {
    case Submit(s, replyTo) =>
      s.prefix match {
        case ac.initCK =>
          replyTo ! Completed

        case ac.datumCK =>
          if (isHCDAvailable) {
            currentCommand = new DatumCommand(ctx, s, tromboneHCD, currentState, Some(tromboneStateActor))
            mode = Mode.Executing
            ctx.self ! CommandStart(replyTo)
          } else hcdNotAvailableResponse(Some(replyTo))

        case ac.moveCK =>
          if (isHCDAvailable) {
            currentCommand = new MoveCommand(ctx, ac, s, tromboneHCD, currentState, Some(tromboneStateActor))
            mode = Mode.Executing
            ctx.self ! CommandStart(replyTo)
          } else hcdNotAvailableResponse(Some(replyTo))

        case ac.positionCK =>
          if (isHCDAvailable) {
            currentCommand = new PositionCommand(ctx, ac, s, tromboneHCD, currentState, Some(tromboneStateActor))
            mode = Mode.Executing
            ctx.self ! CommandStart(replyTo)
          } else hcdNotAvailableResponse(Some(replyTo))

        case ac.setElevationCK =>
          setElevationItem = s(ac.naElevationKey)
          if (isHCDAvailable) {
            currentCommand = new SetElevationCommand(ctx, ac, s, tromboneHCD, currentState, Some(tromboneStateActor))
            mode = Mode.Executing
            ctx.self ! CommandStart(replyTo)
          } else hcdNotAvailableResponse(Some(replyTo))

        case ac.stopCK =>
          replyTo ! NoLongerValid(
            WrongInternalStateIssue("Trombone assembly must be executing a command to use stop")
          )

        case ac.setAngleCK =>
          replyTo ! NoLongerValid(WrongInternalStateIssue("Trombone assembly must be following for setAngle"))

        case ac.followCK =>
          if (cmd(currentState) == cmdUninitialized
              || (move(currentState) != moveIndexed && move(currentState) != moveMoving)
              || !sodiumLayer(currentState)) {
            replyTo ! NoLongerValid(
              WrongInternalStateIssue(
                s"Assembly state of ${cmd(currentState)}/${move(currentState)}/${sodiumLayer(currentState)} does not allow follow"
              )
            )
          } else {
            val nssItem = s(ac.nssInUseKey)

            followCommandActor = ctx.spawnAnonymous(
              FollowCommand.make(ac, setElevationItem, nssItem, Some(tromboneHCD.componentRef), allEventPublisher)
            )
            mode = Mode.Following
            (tromboneStateActor ? { x: ActorRef[StateWasSet] ⇒
              SetState(cmdContinuous, moveMoving, sodiumLayer(currentState), nssItem.head, x)
            }).onComplete { _ =>
              replyTo ! Completed
            }

            replyTo ! BehaviorChanged[FollowingMsgs](ctx.self)
          }
        case otherCommand =>
          replyTo ! Invalid(
            UnsupportedCommandInStateIssue(
              s"""Trombone assembly does not support the command \"${otherCommand.prefix}\" in the current state."""
            )
          )
      }
    case TromboneStateE(x) ⇒ currentState = x
  }

  def onFollowing(msg: FollowingMsgs): Unit = msg match {
    case Submit(s, replyTo) =>
      s.prefix match {
        case ac.datumCK | ac.moveCK | ac.positionCK | ac.followCK | ac.setElevationCK =>
          replyTo ! Invalid(
            WrongInternalStateIssue(
              "Trombone assembly cannot be following for datum, move, position, setElevation, and follow"
            )
          )

        case ac.setAngleCK =>
          Await.ready(
            tromboneStateActor ? { x: ActorRef[StateWasSet] ⇒
              SetState(cmdBusy, move(currentState), sodiumLayer(currentState), nss(currentState), x)
            },
            timeout.duration
          )

          val zenithAngleItem = s(ac.zenithAngleKey)
          followCommandActor ! SetZenithAngle(zenithAngleItem)
          Matchers.executeMatch(ctx, Matchers.idleMatcher, pubSubRef, Some(replyTo)) {
            case Completed =>
              Await.ready(
                tromboneStateActor ? { x: ActorRef[StateWasSet] ⇒
                  SetState(cmdContinuous, move(currentState), sodiumLayer(currentState), nss(currentState), x)
                },
                timeout.duration
              )
            case Error(message) =>
              println(s"setElevation command failed with message: $message")
          }

        case ac.stopCK =>
          followCommandActor ! StopFollowing
          Await.ready(
            tromboneStateActor ? { x: ActorRef[StateWasSet] ⇒
              SetState(cmdReady, moveIndexed, sodiumLayer(currentState), nss(currentState), x)
            },
            timeout.duration
          )
          mode = Mode.NotFollowing
          replyTo ! Completed

        case other => println(s"Unknown config key: $msg")
      }
    case TromboneStateE(x) ⇒ currentState = x

  }

  def onExecuting(msg: ExecutingMsgs): Unit = msg match {
    case CommandStart(replyTo) =>
      if (isHCDAvailable) {
        currentCommand.startCommand().onComplete {
          case Success(result) ⇒ ctx.self ! CommandComplete(replyTo, result)
          case Failure(ex)     ⇒ throw ex // replace with sending a failed message to self
        }
      } else hcdNotAvailableResponse(Some(replyTo))

    case CommandComplete(replyTo, result) ⇒
      replyTo ! result
      currentCommand.stopCurrentCommand()
      mode = Mode.NotFollowing

    case Submit(Setup(ac.commandInfo, ac.stopCK, _), replyTo) =>
      currentCommand.stopCurrentCommand()
      mode = Mode.NotFollowing
      replyTo ! Cancelled

    case TromboneStateE(x) ⇒ currentState = x
    case s: Submit         ⇒
  }

  private def hcdNotAvailableResponse(commandOriginator: Option[ActorRef[CommandExecutionResponse]]): Unit = {
    commandOriginator.foreach(_ ! NoLongerValid(RequiredHCDUnavailableIssue(s"${ac.hcdComponentId} is not available")))
  }
}
