package csw.services.event.perf

import java.util.concurrent.{ExecutorService, Executors}

import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.services.event.perf.model_obs.ModelObsMultiNodeConfig
import csw.services.event.perf.reporter._
import csw.services.event.perf.utils.SystemMonitoringSupport
import csw.services.event.perf.wiring.{TestConfigs, TestWiring}
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.duration.{Duration, DurationDouble, FiniteDuration}
import scala.sys.process.Process

class BasePerfSuite
    extends MultiNodeSpec(ModelObsMultiNodeConfig)
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with ImplicitSender
    with SystemMonitoringSupport
    with BeforeAndAfterAll {

  val testConfigs = new TestConfigs(system.settings.config)
  val testWiring  = new TestWiring(system)

  import testWiring._

  lazy val sharedPublisher: EventPublisher   = publisher
  lazy val sharedSubscriber: EventSubscriber = subscriber
  lazy val reporterExecutor: ExecutorService = Executors.newFixedThreadPool(1)

  var topProcess: Option[Process] = None

  var throughputPlots: PlotResult        = PlotResult()
  var latencyPlots: LatencyPlots         = LatencyPlots()
  var totalDropped: Map[String, Long]    = Map.empty
  var outOfOrderCount: Map[String, Long] = Map.empty

  val defaultTimeout: Duration   = 1.minute
  val maxTimeout: FiniteDuration = 1.hour

  override def initialParticipants: Int = roles.size

  def reporter(name: String): TestRateReporter = {
    val r = new TestRateReporter(name)
    reporterExecutor.execute(r)
    r
  }

  override def beforeAll(): Unit = {
    multiNodeSpecBeforeAll()
    if (testConfigs.systemMonitoring) startSystemMonitoring()
  }

  override def afterAll(): Unit = {
    reporterExecutor.shutdown()
    runOn(roles.last) {
      throughputPlots.printTable()
      latencyPlots.printTable()
      printTotalDropped()
      printTotalOutOfOrderCount()
    }
    topProcess.foreach { top ⇒
      top.destroy()
      plotCpuUsageGraph()
      plotMemoryUsageGraph()
    }
    multiNodeSpecAfterAll()
  }

  def startSystemMonitoring(): Unit = {
    runJstat()
    runPerfFlames(roles: _*)(delay = 15.seconds, time = 60.seconds)
    topProcess = runTop()
  }

  def aggregateResult(testName: String, aggregatedResult: AggregatedResult): Unit = {
    latencyPlots = latencyPlots.copy(
      plot50 = latencyPlots.plot50.addAll(aggregatedResult.latencyPlots.plot50),
      plot90 = latencyPlots.plot90.addAll(aggregatedResult.latencyPlots.plot90),
      plot99 = latencyPlots.plot99.addAll(aggregatedResult.latencyPlots.plot99)
    )

    throughputPlots = throughputPlots.addAll(aggregatedResult.throughputPlots)

    totalDropped = totalDropped + (testName       → aggregatedResult.totalDropped)
    outOfOrderCount = outOfOrderCount + (testName → aggregatedResult.outOfOrderCount)
  }

  def printTotalOutOfOrderCount(): Unit = {
    println("================================ Out of order =================================")
    outOfOrderCount.foreach {
      case (testName, outOfOrder) ⇒ println(s"$testName: ${if (testName.length < 7) "\t\t" else "\t"} $outOfOrder")
    }
  }

  def printTotalDropped(): Unit = {
    println("================================ Total dropped ================================")
    totalDropped.foreach {
      case (testName, totalDroppedCount) ⇒ println(s"$testName: ${if (testName.length < 7) "\t\t" else "\t"} $totalDroppedCount")
    }
  }

}
