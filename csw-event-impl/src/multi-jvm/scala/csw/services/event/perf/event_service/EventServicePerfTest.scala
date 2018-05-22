package csw.services.event.perf.event_service

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig
import akka.testkit.typed.scaladsl
import com.typesafe.config.ConfigFactory
import csw.services.event.perf.BasePerfSuite
import csw.services.event.perf.reporter._
import csw.services.event.perf.utils.EventUtils
import csw.services.event.perf.utils.EventUtils.nanosToSeconds
import org.HdrHistogram.Histogram

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

class EventServicePerfTest extends BasePerfSuite {

  import testConfigs._
  import testWiring._

  var publisherNodes: immutable.Seq[RoleName]  = roles.take(roles.size / 2)
  var subscriberNodes: immutable.Seq[RoleName] = roles.takeRight(roles.size / 2)

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(subscriberNodes.head) {
      throughputPlots.printTable()
      latencyPlots.printTable()
      printTotalDropped()
      printTotalOutOfOrderCount()
    }
    topProcess.foreach { top ⇒
      top.destroy()
      plotCpuUsageGraph()
      plotMemoryUsageGraph()
      scenarios.foreach(s ⇒ plotLatencyHistogram(s"${BenchmarkFileReporter.targetDirectory.toPath}/${s.name}/Aggregated-*"))
    }
    multiNodeSpecAfterAll()
  }

  def testScenario(scenarioName: String, testSettings: TestSettings): Unit = {
    import testSettings._

    updatePubSubNodes(singlePublisher)

    val subscriberName                                   = testName + "-subscriber"
    val nodeId                                           = myself.name.split("-").last.toInt
    val pubSubAllocationPerNode                          = getPubSubAllocationPerNode(publisherSubscriberPairs)
    val (activeSubscriberNodes, inactiveSubscriberNodes) = activeInactiveSubNodes(pubSubAllocationPerNode)
    val (activePublisherNodes, inactivePublisherNodes)   = activeInactivePubNodes(pubSubAllocationPerNode)
    val inactiveNodes                                    = inactivePublisherNodes ++ inactiveSubscriberNodes

    runOn(activeSubscriberNodes: _*) {
      val subIds          = pubSubAllocationPerNode(nodeId - publisherNodes.size - 1)
      val rep             = reporter(testName)
      val completionProbe = scaladsl.TestProbe[AggregatedResult]()(system.toTyped)

      var totalTimePerNode            = 0L
      var eventsReceivedPerNode       = 0L
      val histogramPerNode: Histogram = new Histogram(SECONDS.toNanos(10), 3)

      runOn(activeSubscriberNodes.head) {
        val resultAggregator =
          new ResultAggregator(scenarioName, testName, subscriber, activeSubscriberNodes.size, completionProbe.ref)
        Await.result(resultAggregator.startSubscription().ready(), 30.seconds)
      }

      val subscribers = subIds.map { n ⇒
        val pubId      = if (singlePublisher) 1 else n
        val subscriber = new Subscriber(testSettings, testConfigs, rep, pubId, n, testWiring, sharedSubscriber)
        (subscriber.startSubscription(), subscriber)
      }
      enterBarrier(subscriberName + "-started")

      var totalDroppedPerSubscriber: Long    = 0L
      var outOfOrderCountPerSubscriber: Long = 0L

      subscribers.foreach {
        case (doneF, subscriber) ⇒
          Await.result(doneF, 20.minute)
          subscriber.printResult()

          outOfOrderCountPerSubscriber += subscriber.outOfOrderCount
          totalDroppedPerSubscriber += subscriber.totalDropped()

          histogramPerNode.add(subscriber.histogram)
          eventsReceivedPerNode += subscriber.eventsReceived
          totalTimePerNode = Math.max(totalTimePerNode, subscriber.totalTime)
      }

      val byteBuffer: ByteBuffer = ByteBuffer.allocate(326942)
      histogramPerNode.encodeIntoByteBuffer(byteBuffer)

      Await.result(
        publisher.publish(
          EventUtils.perfResultEvent(
            byteBuffer.array(),
            eventsReceivedPerNode / nanosToSeconds(totalTimePerNode),
            totalDroppedPerSubscriber,
            outOfOrderCountPerSubscriber
          )
        ),
        30.seconds
      )

      runOn(activeSubscriberNodes.head) {
        val aggregatedResult = completionProbe.expectMessageType[AggregatedResult](5.minute)
        aggregateResult(testName, aggregatedResult)
      }

      enterBarrier(testName + "-done")
      rep.halt()
    }

    runOn(activePublisherNodes: _*) {
      println(
        "========================================================================================================================================="
      )
      println(
        s"[$testName]: Starting benchmark with ${if (singlePublisher) 1 else publisherSubscriberPairs} publishers & $publisherSubscriberPairs subscribers $totalTestMsgs messages with " +
        s"throttling of $elements msgs/${per.toSeconds}s " +
        s"and payload size $payloadSize bytes"
      )
      println(
        "========================================================================================================================================="
      )

      enterBarrier(subscriberName + "-started")

      val pubIds = if (singlePublisher) List(1) else pubSubAllocationPerNode(nodeId - 1)
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

  private def activeInactivePubNodes(pubSubAllocationPerNode: List[immutable.IndexedSeq[Int]]) = {
    if (publisherNodes.size > pubSubAllocationPerNode.size) publisherNodes.splitAt(pubSubAllocationPerNode.size)
    else (publisherNodes, Seq.empty)
  }

  private def activeInactiveSubNodes(pubSubAllocationPerNode: List[immutable.IndexedSeq[Int]]) = {
    if (subscriberNodes.size > pubSubAllocationPerNode.size) subscriberNodes.splitAt(pubSubAllocationPerNode.size)
    else (subscriberNodes, Seq.empty)
  }

  private def updatePubSubNodes(singlePublisher: Boolean): Unit = {
    if (singlePublisher) {
      publisherNodes = List(roles.head)
      subscriberNodes = roles.tail
    }
  }

  private def getPubSubAllocationPerNode(publisherSubscriberPairs: Int): List[immutable.IndexedSeq[Int]] = {
    (1 to publisherSubscriberPairs)
      .grouped((publisherSubscriberPairs.toFloat / subscriberNodes.size.toFloat).ceil.toInt)
      .toList
  }

  private val scens     = new Scenarios(testConfigs)
  private val scenarios = List(scens.payloadOneToOne, scens.payloadOneToMany)

  for (scenario ← scenarios) {
    val scenarioName = scenario.name
    for (currentTest ← scenario.testSettings)
      test(s"Perf results must be great for ${currentTest.testName} with payloadSize = ${currentTest.payloadSize}") {
        testScenario(scenarioName, currentTest)
      }
  }

}
