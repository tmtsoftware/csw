package csw.logging.client.appenders

import java.net.InetAddress

import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.logging.api.scaladsl._
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.scaladsl.{LoggerFactory, LoggingSystemFactory}
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object MyFavLibraryLogger extends LoggerFactory("MyFavComponent")

class MyFavComponent {

  val log: Logger = MyFavLibraryLogger.getLogger

  def startLogging(): Unit = {
    log.info("info")
    log.warn("warn")
    log.error("error")
    log.fatal("fatal")
  }
}

class CustomAppenderBuilderClass extends LogAppenderBuilder {
  val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]

  override def apply(factory: ActorRefFactory, standardHeaders: JsObject): LogAppender =
    new CustomAppender(factory, standardHeaders, x ⇒ logBuffer += Json.parse(x.toString).as[JsObject])
}

object CustomAppenderBuilderObject extends LogAppenderBuilder {
  val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]

  override def apply(factory: ActorRefFactory, standardHeaders: JsObject): LogAppender =
    new CustomAppender(factory, standardHeaders, x ⇒ logBuffer += Json.parse(x.toString).as[JsObject])
}

class CustomAppender(factory: ActorRefFactory, stdHeaders: JsObject, callback: Any ⇒ Unit) extends LogAppender {

  private[this] val system = factory match {
    case context: ActorContext => context.system
    case s: ActorSystem        => s
  }
  private[this] val config       = system.settings.config.getConfig("csw-logging.appender-config.my-fav-appender")
  private[this] val logIpAddress = config.getBoolean("logIpAddress")

  override def stop(): Future[Unit] = Future.successful(())

  override def finish(): Future[Unit] = Future.successful(())

  override def append(baseMsg: JsObject, category: String): Unit = {
    if (logIpAddress)
      callback((baseMsg ++ Json.obj("IpAddress" → InetAddress.getLocalHost.getHostAddress)).toString())
    else
      callback(baseMsg.toString())
  }
}

//DEOPSCSW-272: Choose Appenders from Conf file
class CustomAppenderTest extends FunSuite with Matchers {
  private val hostName = InetAddress.getLocalHost.getHostAddress

  test("should be able to add and configure a custom appender using an object extending from CustomAppenderBuilder") {

    val config =
      ConfigFactory.parseString("""
        |include "logging.conf"
        |csw-logging {
        | appenders = ["csw.logging.client.appenders.CustomAppenderBuilderObject$"]
        | appender-config {
        |   my-fav-appender {
        |     logIpAddress = true
        |   }
        | }
        |}
      """.stripMargin)

    val actorSystem   = ActorSystem("test", config)
    val loggingSystem = LoggingSystemFactory.start("foo-name", "foo-version", hostName, actorSystem)
    loggingSystem.setAppenders(List(CustomAppenderBuilderObject))

    new MyFavComponent().startLogging()
    Thread.sleep(200)
    CustomAppenderBuilderObject.logBuffer.size shouldBe 4

    CustomAppenderBuilderObject.logBuffer.forall(log ⇒ log.contains("IpAddress")) shouldBe true
    CustomAppenderBuilderObject.logBuffer.forall(log ⇒ log.getString("IpAddress") == hostName) shouldBe true

    Await.result(actorSystem.terminate(), 10.seconds)
  }

  test("should be able to add and configure a custom appender using a class extending from CustomAppenderBuilder") {

    val config = ConfigFactory.parseString("""
        |include "logging.conf"
        |csw-logging {
        | appenders = ["csw.logging.client.appenders.CustomAppenderBuilderClass"]
        | appender-config {
        |   my-fav-appender {
        |     logIpAddress = true
        |   }
        | }
        |}
      """.stripMargin)

    val actorSystem    = ActorSystem("test", config)
    val loggingSystem  = LoggingSystemFactory.start("foo-name", "foo-version", hostName, actorSystem)
    val customAppender = new CustomAppenderBuilderClass
    loggingSystem.setAppenders(List(customAppender))

    new MyFavComponent().startLogging()
    Thread.sleep(200)
    customAppender.logBuffer.size shouldBe 4

    customAppender.logBuffer.forall(log ⇒ log.contains("IpAddress")) shouldBe true
    customAppender.logBuffer.forall(log ⇒ log.getString("IpAddress") == hostName) shouldBe true

    Await.result(actorSystem.terminate(), 10.seconds)
  }

  // Added this test to show that custom appender config file changes work
  test(
    "should be able to add and configure a custom appender using a class extending from CustomAppenderBuilder showing config changes"
  ) {

    val config = ConfigFactory.parseString("""
                                             |include "logging.conf"
                                             |csw-logging {
                                             | appenders = ["csw.logging.client.appenders.CustomAppenderBuilderClass"]
                                             | appender-config {
                                             |   my-fav-appender {
                                             |     logIpAddress = false
                                             |   }
                                             | }
                                             |}
                                           """.stripMargin)

    val actorSystem    = ActorSystem("test", config)
    val loggingSystem  = LoggingSystemFactory.start("foo-name", "foo-version", hostName, actorSystem)
    val customAppender = new CustomAppenderBuilderClass
    loggingSystem.setAppenders(List(customAppender))

    new MyFavComponent().startLogging()
    Thread.sleep(200)
    customAppender.logBuffer.size shouldBe 4

    customAppender.logBuffer.forall(log ⇒ log.contains("IpAddress")) shouldBe false

    Await.result(actorSystem.terminate(), 10.seconds)
  }
}
