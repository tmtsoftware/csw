package csw.trombone.hcd.actors

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior}
import csw.trombone.hcd._
import csw.trombone.hcd.AxisRequest._
import csw.trombone.hcd.AxisResponse.{AxisStarted, AxisStatistics, AxisUpdate}
import csw.trombone.hcd.InternalMessages.{DatumComplete, HomeComplete, InitialStatistics, MoveComplete}
import csw.trombone.hcd.AxisState.{AXIS_IDLE, AXIS_MOVING}

import scala.concurrent.duration.DurationInt

object AxisSimulator {

  def behavior(axisConfig: AxisConfig, replyTo: Option[ActorRef[AxisResponse]]): Behavior[AxisRequest] =
    Actor.mutable[SimulatorCommand](ctx ⇒ new AxisSimulator(ctx, axisConfig: AxisConfig, replyTo)).narrow

  def limitMove(ac: AxisConfig, request: Int): Int = Math.max(Math.min(request, ac.highLimit), ac.lowLimit)

  def isHighLimit(ac: AxisConfig, current: Int): Boolean = current >= ac.highUser

  def isLowLimit(ac: AxisConfig, current: Int): Boolean = current <= ac.lowUser

  def isHomed(ac: AxisConfig, current: Int): Boolean = current == ac.home

  sealed trait Mode
  object Mode {
    case object Idle                                      extends Mode
    case class Homing(worker: ActorRef[MotionWorkerMsgs]) extends Mode
    case class Moving(worker: ActorRef[MotionWorkerMsgs]) extends Mode
  }
}

class AxisSimulator(
    ctx: ActorContext[SimulatorCommand],
    axisConfig: AxisConfig,
    replyTo: Option[ActorRef[AxisResponse]]
) extends Actor.MutableBehavior[SimulatorCommand] {

  import AxisSimulator._

  assert(axisConfig.home > axisConfig.lowUser,
         s"home position must be greater than lowUser value: ${axisConfig.lowUser}")
  assert(axisConfig.home < axisConfig.highUser,
         s"home position must be less than highUser value: ${axisConfig.highUser}")

  private var current              = axisConfig.startPosition
  private var inLowLimit           = false
  private var inHighLimit          = false
  private var inHome               = false
  private var axisState: AxisState = AXIS_IDLE

  private var initCount    = 0
  private var moveCount    = 0
  private var homeCount    = 0
  private var limitCount   = 0
  private var successCount = 0
  private val failureCount = 0
  private var cancelCount  = 0

  var context: Mode = Mode.Idle

  override def onMessage(msg: SimulatorCommand): Behavior[SimulatorCommand] = {
    (context, msg) match {
      case (Mode.Idle, x: AxisRequest)                ⇒ onIdleRequest(x)
      case (Mode.Idle, x: InternalMessages)           ⇒ onIdleInternal(x)
      case (Mode.Homing(worker), x: MotionWorkerMsgs) ⇒ onHoming(x, worker)
      case (Mode.Moving(worker), x: MotionWorkerMsgs) ⇒ onMoving(x, worker)
      case _                                          ⇒ println(s"current context=$context does not handle message=$msg")
    }
    this
  }

  import MotionWorkerMsgs._

  def onIdleRequest(msg: AxisRequest): Unit = msg match {
    case InitialState(replyToIn) =>
      replyToIn ! getState

    case Datum =>
      axisState = AXIS_MOVING
      update(replyTo, AxisStarted)
      ctx.schedule(1.second, ctx.self, DatumComplete)
      // Stats
      initCount += 1
      moveCount += 1

    case GetStatistics(replyToIn) ⇒
      replyToIn ! AxisStatistics(axisConfig.axisName,
                                 initCount,
                                 moveCount,
                                 homeCount,
                                 limitCount,
                                 successCount,
                                 failureCount,
                                 cancelCount)

    case PublishAxisUpdate =>
      update(replyTo, getState)

    case Home =>
      axisState = AXIS_MOVING
      moveCount += 1
      update(replyTo, AxisStarted)
      println(s"AxisHome: $axisState")

      val workerB = MotionWorker.behavior(current, axisConfig.home, delayInMS = 100, ctx.self, diagFlag = false)
      val worker  = ctx.spawnAnonymous(workerB)

      worker ! Start(ctx.self)
      context = Mode.Homing(worker)

    case Move(position, diagFlag) =>
      axisState = AXIS_MOVING
      moveCount = moveCount + 1
      update(replyTo, AxisStarted)
      println(s"Move: $position")

      val workerB = MotionWorker.behavior(current,
                                          limitMove(axisConfig, position),
                                          delayInMS = axisConfig.stepDelayMS,
                                          ctx.self,
                                          diagFlag)
      val worker = ctx.spawn(workerB, s"moveWorker-${System.currentTimeMillis}")

      worker ! Start(ctx.self)
      context = Mode.Moving(worker)

    case CancelMove =>
      println("Received Cancel Move while idle :-(")
      cancelCount = cancelCount + 1

  }

  def onIdleInternal(internalMessages: InternalMessages): Unit = internalMessages match {
    case DatumComplete =>
      axisState = AXIS_IDLE
      current += 1
      checkLimits()
      successCount += 1
      update(replyTo, getState)

    case HomeComplete(position) =>
      axisState = AXIS_IDLE
      current = position
      checkLimits()
      if (inHome) homeCount += 1
      successCount += 1
      update(replyTo, getState)

    case MoveComplete(position) =>
      println("Move Complete")
      axisState = AXIS_IDLE
      current = position
      checkLimits()
      if (inHighLimit || inLowLimit) limitCount += 1
      successCount += 1
      update(replyTo, getState)

    case InitialStatistics =>
  }

  def onHoming(message: MotionWorkerMsgs, worker: ActorRef[MotionWorkerMsgs]): Unit = message match {
    case Start(_) =>
      println("Home Start")

    case End(finalpos) =>
      println("Move End")
      context = Mode.Idle
      ctx.self ! HomeComplete(finalpos)

    case Tick(currentIn) =>
      current = currentIn
      checkLimits()
      update(replyTo, getState)

    case MoveUpdate(destination) =>
    case Cancel                  =>
  }

  def onMoving(messsage: MotionWorkerMsgs, worker: ActorRef[MotionWorkerMsgs]): Unit = messsage match {
    case Start(_) =>
      println("Move Start")

    case Cancel =>
      println("Cancel MOVE")
      worker ! Cancel
      cancelCount = cancelCount + 1
      context = Mode.Idle

    case MoveUpdate(targetPosition) =>
      worker ! MoveUpdate(targetPosition)

    case Tick(currentIn) =>
      current = currentIn
      checkLimits()
      println("Move Update")
      update(replyTo, getState)

    case End(finalpos) =>
      println("Move End")
      context = Mode.Idle
      ctx.self ! MoveComplete(finalpos)

  }

  private def checkLimits(): Unit = {
    inHighLimit = isHighLimit(axisConfig, current)
    inLowLimit = isLowLimit(axisConfig, current)
    inHome = isHomed(axisConfig, current)
  }

  private def update(replyTo: Option[ActorRef[AxisResponse]], msg: AxisResponse): Unit = replyTo.foreach(_ ! msg)

  private def getState = AxisUpdate(axisConfig.axisName, axisState, current, inLowLimit, inHighLimit, inHome)
}
