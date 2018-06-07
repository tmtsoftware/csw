package csw.services.event.internal.throttle

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.duration.FiniteDuration

class RateLimiterStage[A](delay: FiniteDuration) extends GraphStage[FlowShape[A, A]] {
  final val in    = Inlet.create[A]("DroppingThrottle.in")
  final val out   = Outlet.create[A]("DroppingThrottle.out")
  final val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    private var open = false

    override def preStart(): Unit = schedulePeriodically(None, delay)

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        if (open) pull(in) //drop
        else {
          push(out, grab(in))
          open = true
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })

    override def onTimer(key: Any): Unit =
      open = false
  }
}
