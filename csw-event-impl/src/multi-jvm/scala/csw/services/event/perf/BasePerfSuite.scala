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
import scala.sys.process.{FileProcessLogger, Process}

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

  var throughputPlots: ThroughputPlots              = ThroughputPlots()
  var latencyPlots: LatencyPlots                    = LatencyPlots()
  var jstatProcessLogger: Option[FileProcessLogger] = None

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
    if (testConfigs.systemMonitoring) {
      topProcess.foreach { top ⇒
        top.destroy()
        plotCpuUsageGraph()
        plotMemoryUsageGraph()
      }
      jstatProcessLogger.foreach { p ⇒
        p.flush()
        p.close()
      }
      plotJstat().foreach(_.exitValue())
    }
    multiNodeSpecAfterAll()
  }

  def startSystemMonitoring(): Unit = {
    jstatProcessLogger = runJstat()
    runPerfFlames(roles: _*)(delay = 15.seconds, time = 60.seconds)
    topProcess = runTop()
  }

  def aggregateResult(testName: String, aggregatedResult: AggregatedResult): Unit = {
    latencyPlots = latencyPlots.copy(
      plot50 = latencyPlots.plot50.addAll(aggregatedResult.latencyPlots.plot50),
      plot90 = latencyPlots.plot90.addAll(aggregatedResult.latencyPlots.plot90),
      plot99 = latencyPlots.plot99.addAll(aggregatedResult.latencyPlots.plot99),
      avg = latencyPlots.avg.addAll(aggregatedResult.latencyPlots.avg)
    )

    throughputPlots = throughputPlots.copy(
      throughput = throughputPlots.throughput.addAll(aggregatedResult.throughputPlots.throughput),
      dropped = throughputPlots.dropped.addAll(aggregatedResult.throughputPlots.dropped),
      outOfOrder = throughputPlots.outOfOrder.addAll(aggregatedResult.throughputPlots.outOfOrder)
    )
  }

}
