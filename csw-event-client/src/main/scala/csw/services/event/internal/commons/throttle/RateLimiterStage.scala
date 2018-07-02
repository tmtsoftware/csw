package csw.services.event.internal.commons.throttle

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.duration.FiniteDuration

/**
 * Stream processing stage which provides a stream limiting the rate of flowing elements while providing the element
 * with minimum latency as soon as it is received from upstream. It does not ensure an element is available downstream
 * to match the required frequency. It will not provide any element if it is not available from upstream
 * It drops the elements in case the elements are received at a rate higher than delay.
 * @param delay the duration determining the frequency/rate of elements
 */
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
