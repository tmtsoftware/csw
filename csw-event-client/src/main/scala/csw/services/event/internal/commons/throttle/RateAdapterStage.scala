package csw.services.event.internal.commons.throttle

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.duration.FiniteDuration

class RateAdapterStage[A](delay: FiniteDuration) extends GraphStage[FlowShape[A, A]] {
  final val in    = Inlet.create[A]("DroppingThrottle.in")
  final val out   = Outlet.create[A]("DroppingThrottle.out")
  final val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    private var isPulled             = false
    private var firstTick            = true
    private var maybeElem: Option[A] = None

    override def preStart(): Unit = {
      schedulePeriodically(None, delay)
      pull(in)
    }

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        val ele = grab(in)
        maybeElem = Some(ele)
        if (firstTick) { push(out, ele); firstTick = false }
        pull(in) //drop
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit =
        isPulled = true
    })

    override def onTimer(key: Any): Unit = {
      if (isPulled) maybeElem.foreach { x =>
        isPulled = false
        push(out, x)
      }
    }
  }
}
