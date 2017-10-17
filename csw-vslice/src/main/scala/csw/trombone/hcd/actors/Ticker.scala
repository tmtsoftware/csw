package csw.trombone.hcd.actors

import akka.typed.scaladsl.{Actor, ActorContext, TimerScheduler}
import csw.trombone.hcd.actors.TickerMsgs.{Cancel, End, Tick, UpdateTick}

import scala.concurrent.duration.FiniteDuration

sealed trait TickerMsgs
object TickerMsgs {
  case class Tick()                extends TickerMsgs
  case class UpdateTick(tick: Int) extends TickerMsgs
  case object Cancel               extends TickerMsgs
  case object End                  extends TickerMsgs
}

class Ticker(
    ctx: ActorContext[TickerMsgs],
    timerScheduler: TimerScheduler[TickerMsgs],
    delay: FiniteDuration,
    tickHandler: TickHandler,
    timerKey: String
) extends Actor.MutableBehavior[TickerMsgs] {
  var ticks: Int = 0
  var cancelFlag = false
  timerScheduler.startSingleTimer(timerKey, Tick(), delay)
  override def onMessage(msg: TickerMsgs): Ticker = {
    msg match {
      case Tick()           => tickHandler.onTick()
      case UpdateTick(tick) => ticks += tick
      case Cancel           ⇒ cancelFlag = true
      case End ⇒
        timerScheduler.cancel(timerKey)
        tickHandler.onEnd()
    }
    this
  }
}
