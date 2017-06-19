package csw.services.logging.appenders

import java.io.ByteArrayOutputStream

import akka.actor.ActorSystem
import com.persist.JsonOps
import csw.services.logging.RichMsg
import org.scalatest.{BeforeAndAfterEach, FunSuite, Matchers}

// DEOPSCSW-122: Allow local component logs to be output to STDOUT
class StdOutAppenderTest extends FunSuite with Matchers with BeforeAndAfterEach {

  private val actorSystem = ActorSystem("test-actor-system")

  private val standardHeaders: Map[String, RichMsg] = Map[String, RichMsg]("@version" -> 1, "@host" -> "localhost",
    "@service" -> Map[String, RichMsg]("name" -> "test-service", "version" -> "1.2.3"))

  private val stdOutAppender = new StdOutAppender(actorSystem, standardHeaders, println)

  val logMessage: String =
    """{
      |"@componentName":"tromboneHcd",
      | "@severity":"WARN",
      | "@version":1,
      | "@msg":"This is a test log message.",
      | "class":"csw.services.logging.Class2",
      | }
    """.stripMargin

  val expectedLogJson = JsonOps.Json(logMessage).asInstanceOf[Map[String, String]]

  val outCapture = new ByteArrayOutputStream

  override protected def afterEach(): Unit = {
    outCapture.reset()
  }

  test("StdOutAppender prints message to standard output stream if category is \'common\'") {
    val category = "common"

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, category)
    }

    val actualLogJson = JsonOps.Json(outCapture.toString).asInstanceOf[Map[String, String]]
    actualLogJson shouldBe expectedLogJson
  }

  test("StdOutAppender should not print message to standard output stream if category is not \'common\'") {
    val category = "foo"

    Console.withOut(outCapture) {
      stdOutAppender.append(expectedLogJson, category)
    }

    outCapture.toString.isEmpty shouldBe true
  }

}
