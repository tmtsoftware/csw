package csw.services.logging.appenders

import java.io.ByteArrayOutputStream

import akka.actor.ActorSystem
import com.persist.JsonOps
import com.typesafe.config.ConfigFactory
import csw.services.logging.RichMsg
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-122: Allow local component logs to be output to STDOUT
class StdOutAppenderTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  private val actorSystem = ActorSystem("test-1")

  private val standardHeaders: Map[String, RichMsg] = Map[String, RichMsg]("@version" -> 1, "@host" -> "localhost",
    "@service" -> Map[String, RichMsg]("name" -> "test-service", "version" -> "1.2.3"))

  private val stdOutAppender = new StdOutAppender(actorSystem, standardHeaders, println)

  val logMessage: String =
    """{
      |"@componentName":"tromboneHcd",
      | "@severity":"WARN",
      | "@version":1,
      | "msg":"This is a test log message.",
      | "class":"csw.services.logging.Class2",
      | }
    """.stripMargin

  val expectedLogJson = JsonOps.Json(logMessage).asInstanceOf[Map[String, String]]

  val outCapture = new ByteArrayOutputStream

  override protected def afterEach(): Unit = {
    outCapture.reset()
  }

  override protected def afterAll(): Unit = {
    outCapture.close()
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  test("should print message to standard output stream if category is \'common\'") {
    val category = "common"

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, category)
    }

    val actualLogJson = JsonOps.Json(outCapture.toString).asInstanceOf[Map[String, String]]
    actualLogJson shouldBe expectedLogJson
  }

  test("should not print message to standard output stream if category is not \'common\'") {
    val category = "foo"

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, category)
    }

    outCapture.toString.isEmpty shouldBe true
  }

  test("should able to pretty-print one log message to one line") {

    val config = ConfigFactory
      .parseString("com.persist.logging.appenders.stdout.oneLine=true")
      .withFallback(ConfigFactory.load)

    val system          = ActorSystem("test-2", config)
    val stdOutAppender1 = new StdOutAppender(system, standardHeaders, println)

    Console.withOut(outCapture) {
      stdOutAppender1.append(expectedLogJson, "common")
    }

    val actualOneLineLogMsg   = outCapture.toString.replace("\n", "")
    val expectedOneLineLogMsg = s"[${expectedLogJson.get("@severity").get}] ${expectedLogJson.get("msg").get}"

    actualOneLineLogMsg shouldBe expectedOneLineLogMsg

    Await.result(system.terminate(), 5.seconds)
  }

}
