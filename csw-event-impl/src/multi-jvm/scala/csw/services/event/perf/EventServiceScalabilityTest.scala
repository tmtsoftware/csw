package csw.services.event.perf

import java.io.PrintStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import csw.messages.events.{Event, SystemEvent}
import csw.services.event.perf.EventUtils.{nanosToMicros, nanosToSeconds}
import org.HdrHistogram.Histogram
import org.scalatest._

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

object EventServiceScalabilityTest extends MultiNodeConfig {

  val totalNumberOfNodes: Int =
    System.getProperty("akka.test.FanInThroughputSpec.nrOfNodes") match {
      case null  ⇒ 4
      case value ⇒ value.toInt
    }

  for (n ← 1 to totalNumberOfNodes) role("node-" + n)

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))
}

class EventServiceScalabilityTestMultiJvmNode1 extends EventServiceScalabilityTest
class EventServiceScalabilityTestMultiJvmNode2 extends EventServiceScalabilityTest
class EventServiceScalabilityTestMultiJvmNode3 extends EventServiceScalabilityTest
class EventServiceScalabilityTestMultiJvmNode4 extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode5 extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode6 extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode7  extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode8  extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode9  extends EventServiceScalabilityTest
//class EventServiceScalabilityTestMultiJvmNode10 extends EventServiceScalabilityTest

class EventServiceScalabilityTest
    extends MultiNodeSpec(EventServiceScalabilityTest)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  val testConfigs = new TestConfigs(system.settings.config)
  val wiring      = new TestWiring(system)
  import testConfigs._
  import wiring._

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  override def initialParticipants: Int = roles.size

  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  var throughputPlots: PlotResult = PlotResult()
  var latencyPlots: LatencyPlots  = LatencyPlots()

  var publisherNodes: immutable.Seq[RoleName]  = roles.take(roles.size / 2)
  var subscriberNodes: immutable.Seq[RoleName] = roles.takeRight(roles.size / 2)

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(subscriberNodes.head) {
      println("================================ Throughput msgs/s ================================")
      throughputPlots.printTable()
      println()
      latencyPlots.printTable(system.name)
    }
    multiNodeSpecAfterAll()
  }

  def testScenario(testSettings: TestSettings): Unit = {
    import testSettings._
    val subscriberName = testName + "-subscriber"
    if (singlePublisher) {
      publisherNodes = List(roles.head)
      subscriberNodes = roles.tail
    }

//    runPerfFlames(roles: _*)(delay = 5.seconds, time = 40.seconds)

    val nodeId = myself.name.split("-").tail.head.toInt

    val pubSubAllocationPerNode =
      (1 to publisherSubscriberPairs)
        .grouped((publisherSubscriberPairs.toFloat / subscriberNodes.size.toFloat).ceil.toInt)
        .toList

    val (activeSubscriberNodes, inactiveSubscriberNodes) =
      if (subscriberNodes.size > pubSubAllocationPerNode.size) subscriberNodes.splitAt(pubSubAllocationPerNode.size)
      else (subscriberNodes, Seq.empty)

    val (activePublisherNodes, inactivePublisherNodes) =
      if (publisherNodes.size > pubSubAllocationPerNode.size) publisherNodes.splitAt(pubSubAllocationPerNode.size)
      else (publisherNodes, Seq.empty)

    val inactiveNodes = inactivePublisherNodes ++ inactiveSubscriberNodes

    runOn(activeSubscriberNodes: _*) {
      val subIds          = pubSubAllocationPerNode(nodeId - publisherNodes.size - 1)
      val rep             = reporter(testName)
      val completionProbe = TestProbe()

      var totalTimePerNode            = 0L
      var eventsReceivedPerNode       = 0L
      val histogramPerNode: Histogram = new Histogram(SECONDS.toNanos(10), 3)

      var aggregatedThroughput: Double   = 0
      var aggregatedHistogram: Histogram = null

      runOn(activeSubscriberNodes.head) {
        aggregatedHistogram = new Histogram(SECONDS.toNanos(10), 3)
        var newEvent               = false
        var receivedPerfEventCount = 0

        def onEvent(event: Event): Unit = event match {
          case event: SystemEvent if newEvent ⇒
            receivedPerfEventCount += 1
            val histogramBuffer = ByteString(event.get(EventUtils.histogramKey).get.values).asByteBuffer
            aggregatedHistogram.add(Histogram.decodeFromByteBuffer(histogramBuffer, SECONDS.toNanos(10)))

            aggregatedThroughput += event.get(EventUtils.throughputKey).get.head

            if (receivedPerfEventCount == activeSubscriberNodes.size) completionProbe.ref ! "completed"
          case _ ⇒ newEvent = true
        }

        val eventSubscription = subscriber.subscribeCallback(Set(EventUtils.perfEventKey), onEvent)
        Await.result(eventSubscription.ready, 5.seconds)
      }

      val subscribers = subIds.map { n ⇒
        val pubId      = if (singlePublisher) 1 else n
        val subscriber = new Subscriber(testSettings, testConfigs, rep, pubId, n)
        val doneF      = subscriber.startSubscription()
        (doneF, subscriber)
      }
      enterBarrier(subscriberName + "-started")

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, 5.minute)
          subscriber.printResult()

          histogramPerNode.add(subscriber.histogram)
          eventsReceivedPerNode += subscriber.eventsReceived
          totalTimePerNode = Math.max(totalTimePerNode, subscriber.totalTime)
      }

      val byteBuffer: ByteBuffer = ByteBuffer.allocate(1358140)
      histogramPerNode.encodeIntoByteBuffer(byteBuffer)

      Await.result(
        publisher.publish(
          EventUtils.perfResultEvent(byteBuffer.array(), eventsReceivedPerNode / nanosToSeconds(totalTimePerNode))
        ),
        5.seconds
      )

      runOn(activeSubscriberNodes.head) {
        completionProbe.receiveOne(5.seconds)
        aggregateResult(testSettings, aggregatedThroughput, aggregatedHistogram)
      }

      enterBarrier(testName + "-done")
      rep.halt()
    }

    runOn(activePublisherNodes: _*) {
      val pubIds = if (singlePublisher) List(1) else pubSubAllocationPerNode(nodeId - 1)

      println(
        "================================================================================================================================================"
      )
      println(
        s"[$testName]: Starting benchmark with ${if (singlePublisher) 1 else publisherSubscriberPairs} publishers & $publisherSubscriberPairs subscribers $totalTestMsgs messages with " +
        s"throttling of $elements msgs/${per.toSeconds}s " +
        s"and payload size $payloadSize bytes"
      )
      println(
        "================================================================================================================================================"
      )

      enterBarrier(subscriberName + "-started")

      pubIds.foreach(id ⇒ new Publisher(testSettings, testConfigs, id).startPublishing())

      enterBarrier(testName + "-done")
    }

    runOn(inactiveNodes: _*) {
      enterBarrier(subscriberName + "-started")
      enterBarrier(testName + "-done")
    }

    enterBarrier("after-" + testName)
  }

  private def aggregateResult(
      testSettings: TestSettings,
      throughput: Double,
      aggregatedHistogram: Histogram
  ): Unit = {
    import testSettings._

    throughputPlots = throughputPlots.addAll(PlotResult().add(testName, throughput))

    def percentile(p: Double): Double = nanosToMicros(aggregatedHistogram.getValueAtPercentile(p))

    val latencyPlotsTmp = LatencyPlots(
      PlotResult().add(testName, percentile(50.0)),
      PlotResult().add(testName, percentile(90.0)),
      PlotResult().add(testName, percentile(99.0))
    )

    latencyPlots = latencyPlots.copy(
      plot50 = latencyPlots.plot50.addAll(latencyPlotsTmp.plot50),
      plot90 = latencyPlots.plot90.addAll(latencyPlotsTmp.plot90),
      plot99 = latencyPlots.plot99.addAll(latencyPlotsTmp.plot99)
    )

    aggregatedHistogram.outputPercentileDistribution(
      new PrintStream(BenchmarkFileReporter(s"Aggregated-$testName", system, logSettings = false).fos),
      1000.0
    )
  }

  private val scenarios = new Scenarios(testConfigs)

  for (s ← scenarios.payload) {
    test(s"Perf results must be great for ${s.testName} with payloadSize = ${s.payloadSize}") {
      testScenario(s)
    }
  }

}
