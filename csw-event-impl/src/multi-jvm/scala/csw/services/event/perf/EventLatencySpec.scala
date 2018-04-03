package csw.services.event.perf

import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.{PoisonPill, Props, Terminated}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.services.event.RedisFactory
import csw.services.event.internal.commons.Wiring
import csw.services.event.perf.EventUtils._
import csw.services.event.perf.testkit.RemotingMultiNodeSpec
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.HdrHistogram.Histogram
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object EventLatencySpec extends MultiNodeConfig {
  val first: RoleName  = role("first")
  val second: RoleName = role("second")

  val barrierTimeout: FiniteDuration = 5.minutes

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.parseString(s"""
       # for serious measurements you should increase the totalMessagesFactor (30) and repeatCount (3)
       akka.test.LatencySpec.totalMessagesFactor = 10
       akka.test.LatencySpec.repeatCount = 1
       akka {
         # avoid TestEventListener
         loggers = ["akka.event.Logging$$DefaultLogger"]
         testconductor.barrier-timeout = ${barrierTimeout.toSeconds}s
         actor {
           provider = remote
           serialize-creators = false
           serialize-messages = false
         }
       }
       """)).withFallback(RemotingMultiNodeSpec.commonConfig))

  final case class LatencyTestSettings(testName: String, messageRate: Int, payloadSize: Int, repeat: Int)

}

class EventLatencySpecMultiJvmNode1 extends EventLatencySpec

class EventLatencySpecMultiJvmNode2 extends EventLatencySpec

abstract class EventLatencySpec extends RemotingMultiNodeSpec(EventLatencySpec) with MockitoSugar {

  import EventLatencySpec._

  val totalMessagesFactor: Double = system.settings.config.getDouble("akka.test.LatencySpec.totalMessagesFactor")
  val repeatCount: Int            = system.settings.config.getInt("akka.test.LatencySpec.repeatCount")

  var plots = LatencyPlots()

  lazy implicit val mat: ActorMaterializer = ActorMaterializer()(system)

  override def initialParticipants: Int = roles.size

  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(second) {
      println(plots.plot50.csv(system.name + "50"))
      println(plots.plot90.csv(system.name + "90"))
      println(plots.plot99.csv(system.name + "99"))
    }
    super.afterAll()
  }

  val scenarios = List(
    LatencyTestSettings(testName = "warmup", messageRate = 5000, payloadSize = 100, repeat = repeatCount)
//    LatencyTestSettings(testName = "rate-100-size-100", messageRate = 100, payloadSize = 100, repeat = repeatCount),
//    LatencyTestSettings(testName = "rate-1000-size-100", messageRate = 1000, payloadSize = 100, repeat = repeatCount),
//    LatencyTestSettings(testName = "rate-10000-size-100", messageRate = 10000, payloadSize = 100, repeat = repeatCount),
//    LatencyTestSettings(testName = "rate-20000-size-100", messageRate = 20000, payloadSize = 100, repeat = repeatCount),
//    LatencyTestSettings(testName = "rate-1000-size-1k", messageRate = 1000, payloadSize = 1000, repeat = repeatCount)
  )

  def test(testSettings: LatencyTestSettings, benchmarkFileReporter: BenchmarkFileReporter): Unit = {

    import testSettings._

    val histogram     = new Histogram(SECONDS.toNanos(10), 3)
    val totalMessages = (2 * messageRate * totalMessagesFactor).toInt

    runOn(first) {

      implicit val ec: ExecutionContext = system.dispatcher

      val redisHost = "localhost"
      val redisPort = 6379
      val publisher = redisFactory.publisher(redisHost, redisPort)

      val payload = ("0" * payloadSize).getBytes("utf-8")
      enterBarrier("subscribed")

      // warmup for 3 seconds to init compression
      val warmup = Source(1 to 30)
        .throttle(10, 1.second, 10, ThrottleMode.Shaping)
        .runForeach { n ⇒
          Await.result(publisher.publish(EventUtils.latencyWarmUpEvent), 5.seconds)
        }

      warmup.foreach { _ ⇒
        val source = Source(0 until totalMessages)
          .throttle(messageRate, 1.seconds, messageRate, ThrottleMode.Shaping)
          .map(i ⇒ eventWithNanos(EventName(testEvent), i, payload))
          .watchTermination()(Keep.right)

        publisher.publish(source).onComplete(_ ⇒ publisher.publish(event(endEvent)))
      }
      enterBarrier("received-all-events")
    }

    runOn(second) {

      val redisHost  = "localhost"
      val redisPort  = 6379
      val subscriber = redisFactory.subscriber(redisHost, redisPort)

      val testProbe = TestProbe()
      val ref       = system.actorOf(Props.empty)
      testProbe.watch(ref)

      var count            = 0
      var repeatCount      = 0
      var reportedArrayOOB = false
      val rep              = reporter(testName)
      var startTime: Long  = System.nanoTime()

      val keys: Set[EventKey] = Set(
        EventKey(s"${prefix.prefix}.$warmupEvent"),
        EventKey(s"$testEventKey"),
        EventKey(s"${prefix.prefix}.$endEvent")
      )

      def startSubscription(eventKeys: Set[EventKey]) = subscriber.subscribeCallback(eventKeys, onEvent)
      startSubscription(keys)

      enterBarrier("subscribed")

      def onEvent(event: Event): Unit = {
        event match {
          case SystemEvent(_, _, `warmupEvent`, _, _) ⇒
          case SystemEvent(_, _, `endEvent`, _, _) ⇒
            if (count > 10) {
              repeatCount += 1
              printTotal(testName, payloadSize, histogram, System.nanoTime() - startTime, benchmarkFileReporter)
              if (repeatCount == repeat) {
                println(s"=== $testName: total events dropped: ${totalMessages - count}")
                ref ! PoisonPill
              }
            }
          case Event.invalidEvent ⇒
          case event: SystemEvent ⇒ processEvent(event.get(timeNanosKey).get.head, payloadSize)
          case _                  ⇒
        }
      }

      def processEvent(sendTime: Long, size: Int): Unit = {
        if (count == 0)
          startTime = System.nanoTime()
        rep.onMessage(1, payloadSize)
        count += 1
        val d = System.nanoTime() - sendTime
        try {
          histogram.recordValue(d)
        } catch {
          case e: ArrayIndexOutOfBoundsException ⇒
            // Report it only once instead of flooding the console
            if (!reportedArrayOOB) {
              e.printStackTrace()
              reportedArrayOOB = true
            }
        }
      }

      def printTotal(
          testName: String,
          payloadSize: Long,
          histogram: Histogram,
          totalDurationNanos: Long,
          reporter: BenchmarkFileReporter
      ): Unit = {
        def percentile(p: Double): Double = histogram.getValueAtPercentile(p) / 1000.0
        val throughput                    = 1000.0 * histogram.getTotalCount / math.max(1, totalDurationNanos.nanos.toMillis)

        reporter.reportResults(
          s"===== ${reporter.testName} $testName ===== \n" +
          f"    50%%ile: ${percentile(50.0)}%.0f µs \n" +
          f"    90%%ile: ${percentile(90.0)}%.0f µs \n" +
          f"    99%%ile: ${percentile(99.0)}%.0f µs \n" +
          f"    rate: $throughput%,.0f msg/s \n" +
          "======================================"
        )
        println("Histogram of RTT latencies in microseconds.")
        histogram.outputPercentileDistribution(System.out, 1000.0)

//        taskRunnerMetrics.printHistograms()

        val plotsTmp = LatencyPlots(PlotResult().add(testName, percentile(50.0)),
                                    PlotResult().add(testName, percentile(90.0)),
                                    PlotResult().add(testName, percentile(99.0)))

        if (repeatCount == repeat) {
          plots = plots.copy(plot50 = plots.plot50.addAll(plotsTmp.plot50),
                             plot90 = plots.plot90.addAll(plotsTmp.plot90),
                             plot99 = plots.plot99.addAll(plotsTmp.plot99))
        }
      }

      testProbe.expectMsgType[Terminated](5.minutes)
      enterBarrier("received-all-events")
      rep.halt()
    }
    enterBarrier("after-" + testName)
  }

  private def redisFactory = {
    val redisClient  = RedisClient.create()
    val wiring       = new Wiring(system)
    val redisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
    redisFactory
  }

  "Latency of Event Service" must {
    val reporter = BenchmarkFileReporter("EventLatencySpec", system)

    for (s ← scenarios) {
      s"be low for ${s.testName}, at ${s.messageRate} msg/s, payloadSize = ${s.payloadSize}" in test(s, reporter)
    }
  }
}
