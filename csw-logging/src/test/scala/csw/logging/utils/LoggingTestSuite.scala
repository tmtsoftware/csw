package csw.logging.utils

import java.net.InetAddress

import akka.actor.ActorSystem
import csw.logging.internal.LoggingSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class LoggingTestSuite() extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  protected lazy val actorSystem                    = ActorSystem("test")
  protected val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  protected val testAppender                        = new TestAppender(x ⇒ logBuffer += Json.parse(x.toString).as[JsObject])

  private val hostName = InetAddress.getLocalHost.getHostName
  protected lazy val loggingSystem =
    new LoggingSystem("logging", "version", hostName, actorSystem)

  protected val logMsgMap = Map(
    "trace"       → "logging at trace level",
    "debug"       → "logging at debug level",
    "info"        → "logging at info level",
    "warn"        → "logging at warn level",
    "error"       → "logging at error level",
    "fatal"       → "logging at fatal level",
    "alternative" → "logging at alternative level"
  )

  // These extra messages are used to test user keys
  protected val userMsgMap = Map(
    "user1" -> "user1 message",
    "user2" -> "user2 message"
  )

  override protected def beforeAll(): Unit = loggingSystem.setAppenders(List(testAppender))

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = {
    Await.result(loggingSystem.stop, 10.seconds)
    Await.result(actorSystem.terminate(), 5.seconds)
  }

}
