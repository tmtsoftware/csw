package csw.services.logging.scaladsl

import java.net.InetAddress

import com.persist.JsonOps._
import csw.services.logging.utils.TestAppender
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

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

class LoggerTest extends FunSuite with Matchers with BeforeAndAfterAll {

  private val logBuffer    = mutable.Buffer.empty[Json]
  private val testAppender = new TestAppender(x ⇒ logBuffer += Json(x.toString))

  private val hostName      = InetAddress.getLocalHost.getHostName
  private val loggingSystem = new LoggingSystem("logging", "SNAPSHOT-1.0", hostName, Seq(testAppender))

  override protected def afterAll(): Unit =
    Await.result(loggingSystem.stop, 30.seconds)

  // todo : in progress
  test("should load default filter provided in configuration file") {
    new TromboneHcd().startLogging()
    new TromboneAssembly().startLogging()
    Thread.sleep(100)

    logBuffer.size shouldBe 7
  }
}
