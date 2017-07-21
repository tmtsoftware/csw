package csw.trombone.hcd.actors

import akka.typed.{ActorRef, Behavior}
import akka.typed.scaladsl.{Actor, ActorContext}
import csw.trombone.hcd.MotionWorkerMsgs
import csw.trombone.hcd.MotionWorkerMsgs._

import scala.concurrent.duration.DurationLong

object MotionWorker {
  def behaviour(start: Int,
                destinationIn: Int,
                delayInMS: Int,
                replyTo: ActorRef[MotionWorkerMsgs],
                diagFlag: Boolean): Behavior[MotionWorkerMsgs] =
    Actor.mutable(ctx ⇒ new MotionWorker(ctx, start, destinationIn, delayInMS, replyTo, diagFlag))

  def calcNumSteps(start: Int, end: Int): Int = {
    val diff = Math.abs(start - end)
    if (diff <= 5) 1
    else if (diff <= 20) 2
    else if (diff <= 500) 5
    else 10
  }

  def calcStepSize(current: Int, destination: Int, steps: Int): Int = (destination - current) / steps

  def calcDistance(current: Int, destination: Int): Int = Math.abs(current - destination)
  def lastStep(current: Int, destination: Int, stepSize: Int): Boolean =
    calcDistance(current, destination) <= Math.abs(stepSize)
}

class MotionWorker(ctx: ActorContext[MotionWorkerMsgs],
                   start: Int,
                   destinationIn: Int,
                   delayInMS: Int,
                   replyTo: ActorRef[MotionWorkerMsgs],
                   diagFlag: Boolean)
    extends Actor.MutableBehavior[MotionWorkerMsgs] {

  import MotionWorker._

  private var destination = destinationIn

  private var numSteps                 = calcNumSteps(start, destination)
  private var stepSize                 = calcStepSize(start, destination, numSteps)
  private var stepCount                = 0
  private var cancelFlag               = false
  private val delayInNanoSeconds: Long = delayInMS * 1000000
  private var current                  = start

  override def onMessage(msg: MotionWorkerMsgs): Behavior[MotionWorkerMsgs] =
    msg match {
      case Start(replyToIn) =>
        replyTo ! Start(replyTo)
        if (diagFlag) diag("Starting", current, numSteps)
        ctx.schedule(delayInNanoSeconds.nanos, ctx.self, Tick(start + stepSize))
        this
      case Tick(currentIn) =>
        replyTo ! Tick(currentIn)
        current = currentIn
        stepCount += 1
        val distance = calcDistance(current, destination)
        val done     = distance == 0
        val last     = lastStep(current, destination, stepSize)
        val nextPos  = if (last) destination else currentIn + stepSize
        if (diagFlag)
          println(s"currentIn: $currentIn, distance: $distance, stepSize: $stepSize, done: $done, nextPos: $nextPos")

        if (!done && !cancelFlag) ctx.schedule(delayInNanoSeconds.nanos, ctx.self, Tick(nextPos))
        else ctx.self ! End(current)
        this
      case MoveUpdate(destinationIN) =>
        destination = destinationIN
        numSteps = calcNumSteps(current, destination)
        stepSize = calcStepSize(current, destination, numSteps)
        println(s"NEW dest: $destination, numSteps: $numSteps, stepSize: $stepSize")
        this
      case Cancel =>
        if (diagFlag) println("Worker received cancel")
        cancelFlag = true // Will cause to leave on next Tick
        this
      case end @ End(finalpos) =>
        replyTo ! end
        if (diagFlag) diag("End", finalpos, numSteps)
        Actor.stopped
    }

  def diag(hint: String, current: Int, stepValue: Int): Unit =
    println(s"$hint: start=$start, dest=$destination, totalSteps: $stepValue, current=$current")

}
