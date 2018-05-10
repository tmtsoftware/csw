package csw.services.event.perf

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import akka.testkit.typed.scaladsl
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.EventUtils.nanosToSeconds
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import org.HdrHistogram.Histogram
import org.scalatest._

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration._

object EventServiceMultiNodeConfig extends MultiNodeConfig {

  val totalNumberOfNodes: Int =
    System.getProperty("csw.event.perf.nodes") match {
      case null  ⇒ 2
      case value ⇒ value.toInt
    }

  for (n ← 1 to totalNumberOfNodes) role("node-" + n)

  commonConfig(debugConfig(on = false).withFallback(ConfigFactory.load()))

}

class EventServicePerfTestMultiJvmNode1 extends EventServicePerfTest
class EventServicePerfTestMultiJvmNode2 extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode3 extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode4 extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode5 extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode6 extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode7  extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode8  extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode9  extends EventServicePerfTest
//class EventServicePerfTestMultiJvmNode10 extends EventServicePerfTest

class EventServicePerfTest
    extends MultiNodeSpec(EventServiceMultiNodeConfig)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with PerfFlamesSupport
    with BeforeAndAfterAll {

  private val testConfigs = new TestConfigs(system.settings.config)
  import testConfigs._

  private val testWiring = new TestWiring(system)
  import testWiring._

  lazy val sharedPublisher: EventPublisher   = publisher
  lazy val sharedSubscriber: EventSubscriber = subscriber

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

    val nodeId = myself.name.split("-").last.toInt

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
      val completionProbe = scaladsl.TestProbe[AggregatedResult]()(system.toTyped)

      var totalTimePerNode            = 0L
      var eventsReceivedPerNode       = 0L
      val histogramPerNode: Histogram = new Histogram(SECONDS.toNanos(10), 3)

      runOn(activeSubscriberNodes.head) {
        val resultAggregator = new ResultAggregator(testName, subscriber, activeSubscriberNodes.size, completionProbe.ref)
        Await.result(resultAggregator.startSubscription().ready(), 30.seconds)
      }

      val subscribers = subIds.map { n ⇒
        val pubId      = if (singlePublisher) 1 else n
        val subscriber = new Subscriber(testSettings, testConfigs, rep, pubId, n, testWiring, sharedSubscriber)
        val doneF      = subscriber.startSubscription()
        (doneF, subscriber)
      }
      enterBarrier(subscriberName + "-started")

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, 20.minute)
          subscriber.printResult()

          histogramPerNode.add(subscriber.histogram)
          eventsReceivedPerNode += subscriber.eventsReceived
          totalTimePerNode = Math.max(totalTimePerNode, subscriber.totalTime)
      }

      val byteBuffer: ByteBuffer = ByteBuffer.allocate(326942)
      histogramPerNode.encodeIntoByteBuffer(byteBuffer)

      Await.result(
        publisher.publish(
          EventUtils.perfResultEvent(byteBuffer.array(), eventsReceivedPerNode / nanosToSeconds(totalTimePerNode))
        ),
        30.seconds
      )

      runOn(activeSubscriberNodes.head) {
        val aggregatedResult = completionProbe.expectMessageType[AggregatedResult](5.minute)

        latencyPlots = latencyPlots.copy(
          plot50 = latencyPlots.plot50.addAll(aggregatedResult.latencyPlots.plot50),
          plot90 = latencyPlots.plot90.addAll(aggregatedResult.latencyPlots.plot90),
          plot99 = latencyPlots.plot99.addAll(aggregatedResult.latencyPlots.plot99)
        )

        throughputPlots = throughputPlots.addAll(aggregatedResult.throughputPlots)
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

      pubIds.foreach(
        id ⇒ new Publisher(testSettings, testConfigs, id, testWiring, sharedPublisher).startPublishingWithEventGenerator()
      )

      enterBarrier(testName + "-done")
    }

    runOn(inactiveNodes: _*) {
      enterBarrier(subscriberName + "-started")
      enterBarrier(testName + "-done")
    }

    enterBarrier("after-" + testName)
  }

  private val scenarios = new Scenarios(testConfigs)

  for (s ← scenarios.payload) {
    test(s"Perf results must be great for ${s.testName} with payloadSize = ${s.payloadSize}") {
      testScenario(s)
    }
  }

}
