package csw.time.client.api

import java.time.{Duration, Instant}

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.testkit.TestProbe
import csw.time.api.models.UTCTime
import csw.time.client.TimeServiceSchedulerFactory
import org.HdrHistogram.Histogram
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.concurrent.duration.DurationInt

class TimeServiceMultipleSchedulersTest extends ScalaTestWithActorTestKit() with FunSuiteLike with BeforeAndAfterAll {

  private val offset      = 50l // milliseconds
  private val sys         = ActorSystem()
  private val timeService = TimeServiceSchedulerFactory.make()(sys)

  ignore("multiple schedulers") {
    val n = 50
    val xs: List[(TestProbe, Cancellable)] = (1 to n).map { _ =>
      val testProbe = TestProbe()(sys)
      val startTime = UTCTime(UTCTime.now().value.plusSeconds(1L))
      val cancellable: Cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(offset)) {
        testProbe.ref ! UTCTime.now()
      }
      (testProbe, cancellable)
    }.toList

    xs.foreach {
      case (probe, cancellable) =>
        probe.receiveN(1000, 100.seconds)
        val times     = probe.receiveN(1000, 100.seconds).map { case t: UTCTime => t }
        val histogram = new Histogram(3)
        times.zipWithIndex.toList.foreach {
          case (_, i) =>
            val current_time = times(i).value
            val t1_s: Double = current_time.getEpochSecond
            val ns: Double   = current_time.getNano
            val t1           = (t1_s * 1000 * 1000 * 1000) + ns

            val previous_time = if (i == 0) UTCTime(Instant.now) else times(i - 1)
            val t2_s: Double  = previous_time.value.getEpochSecond
            val ns_2: Double  = previous_time.value.getNano
            val t2            = (t2_s * 1000 * 1000 * 1000) + ns_2

            val long = math.abs(((t1 - t2) - (offset * 1000 * 1000)) / 1000).toLong
            histogram.recordValue(long)

        }
        println("98.5 Percentile jitter====" + histogram.getValueAtPercentile(98.5))
        cancellable.cancel()
    }

  }
}
