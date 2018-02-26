package csw.services.event.internal

import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.concurrent.duration.FiniteDuration

class DroppingThrottleStage[A](delay: FiniteDuration) extends GraphStage[FlowShape[A, A]] {
  final val in    = Inlet.create[A]("DroppingThrottle.in")
  final val out   = Outlet.create[A]("DroppingThrottle.out")
  final val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    private var open = false

    override def preStart(): Unit = {
      schedulePeriodically(None, delay)
    }

    setHandler(
      in,
      new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          if (open) {
            pull(in) //drop
          } else {
            push(out, elem)
            open = true
          }
        }
      }
    )

    setHandler(
      out,
      new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      }
    )

    override def onTimer(key: Any): Unit = {
      open = false
    }
  }
}
