package csw.services.logging.utils

import java.net.InetAddress

import com.persist.JsonOps.{Json, JsonObject}
import csw.services.logging.scaladsl.LoggingSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class LoggingTestSuite() extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  protected val logBuffer  = mutable.Buffer.empty[JsonObject]
  private val testAppender = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])

  private val hostName = InetAddress.getLocalHost.getHostName
  protected val loggingSystem =
    new LoggingSystem("logging", "SNAPSHOT-1.0", hostName, appenderBuilders = Seq(testAppender))

  protected val logMsgMap = Map(
    "trace" → "logging at trace level",
    "debug" → "logging at debug level",
    "info"  → "logging at info level",
    "warn"  → "logging at warn level",
    "error" → "logging at error level",
    "fatal" → "logging at fatal level"
  )

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = Await.result(loggingSystem.stop, 10.seconds)

}
