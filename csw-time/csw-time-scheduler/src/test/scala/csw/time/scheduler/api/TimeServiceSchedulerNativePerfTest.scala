/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler.api

import akka.actor.testkit.typed.scaladsl
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Scheduler}
import csw.time.core.models.UTCTime
import csw.time.scheduler.TimeServiceSchedulerFactory
import org.HdrHistogram.Histogram
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class TimeServiceSchedulerNativePerfTest extends AnyFunSuiteLike with BeforeAndAfterAll {

  private val sys                                         = ActorSystem(Behaviors.empty, "test")
  private implicit val executionContext: ExecutionContext = sys.executionContext
  private implicit val scheduler: Scheduler               = sys.scheduler
  private val timeService                                 = new TimeServiceSchedulerFactory().make()

  type PercentileType = (Long, Long, Long)

  private val averageTable =
    new mutable.LinkedHashMap[(Int, Int, Int), (PercentileType, PercentileType)].empty

  def createRow(scenario: (Int, Int, Int), result: PercentileType): String = {
    s"${scenario._1},${scenario._2},${scenario._3},${result._1},${result._2},${result._3}"
  }
  override protected def afterAll(): Unit = {
    val a = averageTable.foldLeft(("", ""))((res, data) => {
      val scenario = data._1
      val result   = data._2
      (res._1 + createRow(scenario, result._1) + "\n", res._2 + createRow(scenario, result._2) + "\n")
    })

    val header = "Offset (in millis),Schedulers,No. of Tasks ran, 50%tile (in µs), 65%tile (in µs), 98.5%tile (in µs)"
    println("jitter calculated from previous tasks")
    println(header)
    println(a._1)

    println("jitter calculated from first tasks")
    println(header)
    println(a._2)
  }

  for (scenario <- TestSettings.all) {
    import scenario.*
    test(s"Offset:$offset Schedulers:$nSchedulers Warmup:$warmup Tasks:$nTasks") {
      val xs: List[(TestProbe[UTCTime], Cancellable)] = (1 to nSchedulers).map { _ =>
        val testProbe = scaladsl.TestProbe[UTCTime]()(sys)
        val startTime = UTCTime(UTCTime.now().value.plusSeconds(1L))
        val cancellable: Cancellable = timeService.schedulePeriodically(startTime, Duration.ofMillis(offset)) {
          testProbe.ref ! UTCTime.now()
        }
        (testProbe, cancellable)
      }.toList

      val diffInMicrosArray: ArrayBuffer[(Long, Long, Long)]   = mutable.ArrayBuffer.empty[PercentileType]
      val jitterINMicrosArray: ArrayBuffer[(Long, Long, Long)] = mutable.ArrayBuffer.empty[PercentileType]

      xs.foreach { case (probe, cancellable) =>
        probe.receiveMessages(warmup, 100.hours) // Do not record warmup tasks

        val times                   = probe.receiveMessages(nTasks, 1.hour).map { t: UTCTime => t }
        val histogram               = new Histogram(3)
        val histogramForConsistency = new Histogram(3)

        times.zipWithIndex.toList.foreach { case (_, i) =>
          val currentTime  = times(i).value
          val t1_s: Double = currentTime.getEpochSecond.toDouble
          val ns: Double   = currentTime.getNano
          val t1           = (t1_s * 1000 * 1000 * 1000) + ns

          val previousTime = if (i == 0) UTCTime(Instant.now) else times(i - 1)
          val t2_s: Double = previousTime.value.getEpochSecond.toDouble
          val ns_2: Double = previousTime.value.getNano
          val t2           = (t2_s * 1000 * 1000 * 1000) + ns_2

          val diff = math.abs(((t1 - t2) - (offset * 1000 * 1000)) / 1000).toLong
          histogram.recordValue(diff)

          val jitterInMicros = math.abs(
            Duration.between(times.head.value.plus(i * offset, ChronoUnit.MILLIS), currentTime).toNanos
          ) / 1000

          histogramForConsistency.recordValue(jitterInMicros)
        }
        diffInMicrosArray.append(
          (
            histogram.getValueAtPercentile(50),
            histogram.getValueAtPercentile(65),
            histogram.getValueAtPercentile(98.5)
          )
        )
        jitterINMicrosArray.append(
          (
            histogramForConsistency.getValueAtPercentile(50),
            histogramForConsistency.getValueAtPercentile(65),
            histogramForConsistency.getValueAtPercentile(98.5)
          )
        )

        println(
          "===========================Jitter(us) in Percentile [WRT Previous-Time | WRT Start-Time] ========================"
        )
        println("50 %tile: " + histogram.getValueAtPercentile(50) + " | " + histogramForConsistency.getValueAtPercentile(50))
        println("65 %tile: " + histogram.getValueAtPercentile(65) + " | " + histogramForConsistency.getValueAtPercentile(65))
        println(
          "98.5 %tile: " + histogram.getValueAtPercentile(98.5) + " | " + histogramForConsistency.getValueAtPercentile(98.5)
        )
        cancellable.cancel()
      }

      averageTable.update((offset, nSchedulers, nTasks), (calcAverage(diffInMicrosArray), calcAverage(jitterINMicrosArray)))
    }
  }

  def calcAverage(list: mutable.ArrayBuffer[PercentileType]): PercentileType = {
    val fiftyPer              = list.map(_._1.toDouble)
    val sixtyFivePer          = list.map(_._2.toDouble)
    val ninetyEightAndHalfPer = list.map(_._3.toDouble)

    (
      Math.round(fiftyPer.sum / fiftyPer.length),
      Math.round(sixtyFivePer.sum / sixtyFivePer.length),
      Math.round(ninetyEightAndHalfPer.sum / ninetyEightAndHalfPer.length)
    )
  }
}
