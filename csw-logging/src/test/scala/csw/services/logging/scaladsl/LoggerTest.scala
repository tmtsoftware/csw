package csw.services.logging.scaladsl

import java.net.InetAddress

import com.persist.JsonOps._
import csw.services.logging.utils.TestAppender
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

object TromboneHcdLogger      extends ComponentLogger(Some("tromboneHcd"))
object TromboneAssemblyLogger extends ComponentLogger(Some("tromboneAssembly"))

class TromboneHcd() extends TromboneHcdLogger.Simple {
  def startLogging(): Unit = {
    log.trace("level: trace")
    log.debug("level: debug")
    log.info("level: info")
    log.warn("level: warn")
    log.error("level: error")
    log.fatal("level: fatal")
  }
}

class TromboneAssembly() extends TromboneAssemblyLogger.Simple {
  def startLogging(): Unit = {
    log.trace("level: trace")
    log.debug("level: debug")
    log.info("level: info")
    log.warn("level: warn")
    log.error("level: error")
    log.fatal("level: fatal")
  }
}

class LoggerTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val logBuffer    = mutable.Buffer.empty[JsonObject]
  private val testAppender = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])

  private val hostName      = InetAddress.getLocalHost.getHostName
  private val loggingSystem = new LoggingSystem("logging", "SNAPSHOT-1.0", hostName, Seq(testAppender))

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = Await.result(loggingSystem.stop, 30.seconds)

  test("component logs should contain component name") {
    new TromboneHcd().startLogging()
    Thread.sleep(100)

    logBuffer.foreach { log ⇒
      log.contains("@componentName") shouldBe true
    }

  }

  test("should load default filter provided in configuration file") {

    /*
      --------------------------------------
      default filters for components are =>
      tromboneHcd = debug
      tromboneAssembly = error
      --------------------------------------
     */

    new TromboneHcd().startLogging()
    new TromboneAssembly().startLogging()
    Thread.sleep(100)

    //  TromboneHcd component is logging 6 messages each of unique level
    //  TromboneAssembly component is logging 6 messages each of unique level
    //  As per the filter, hcd should log 5 message of all level except TRACE
    //  As per the filter, assembly should log 2 message of level (ERROR & FATAL)
    logBuffer.size shouldBe 7

    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs          = groupByComponentNamesLog.get("tromboneHcd").get
    val tromboneAssemblyLogs     = groupByComponentNamesLog.get("tromboneAssembly").get

    tromboneHcdLogs.size shouldBe 5
    tromboneAssemblyLogs.size shouldBe 2

  }
}
