package csw.services.logging.appenders

import java.net.InetAddress

import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import com.persist.JsonOps
import com.persist.JsonOps.{Json, JsonObject}
import com.typesafe.config.ConfigFactory
import csw.services.logging.RichMsg
import csw.services.logging.scaladsl.{ComponentLogger, LoggingSystemFactory}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object MyFavComponentLogger extends ComponentLogger("MyFavComponent")

class MyFavComponent extends MyFavComponentLogger.Simple {
  def startLogging(): Unit = {
    log.info("info")
    log.warn("warn")
    log.error("error")
    log.fatal("fatal")
  }
}

class CustomAppenderBuilderClass extends LogAppenderBuilder {
  val logBuffer: mutable.Buffer[JsonObject] = mutable.Buffer.empty[JsonObject]

  override def apply(factory: ActorRefFactory, standardHeaders: Map[String, RichMsg]): LogAppender =
    new CustomAppender(factory, standardHeaders, x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])
}

object CustomAppenderBuilderObject extends LogAppenderBuilder {
  val logBuffer: mutable.Buffer[JsonObject] = mutable.Buffer.empty[JsonObject]

  override def apply(factory: ActorRefFactory, standardHeaders: Map[String, RichMsg]): LogAppender =
    new CustomAppender(factory, standardHeaders, x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])
}

class CustomAppender(factory: ActorRefFactory, stdHeaders: Map[String, RichMsg], callback: Any ⇒ Unit)
    extends LogAppender {

  private[this] val system = factory match {
    case context: ActorContext => context.system
    case s: ActorSystem        => s
  }
  private[this] val config       = system.settings.config.getConfig("csw-logging.appender-config.my-fav-appender")
  private[this] val logIpAddress = config.getBoolean("logIpAddress")

  override def stop(): Future[Unit] = Future.successful(())

  override def finish(): Future[Unit] = Future.successful(())

  override def append(baseMsg: Map[String, RichMsg], category: String): Unit = {
    if (logIpAddress)
      callback(JsonOps.Compact(baseMsg + "IpAddress" → InetAddress.getLocalHost.getHostAddress))
    else
      callback(JsonOps.Compact(baseMsg))
  }
}

class CustomAppenderTest extends FunSuite with Matchers {
  private val hostName = InetAddress.getLocalHost.getHostAddress

  test("should be able to add and configure a custom appender using an object extending from CustomAppenderBuilder") {

    val config =
      ConfigFactory.parseString("""
        |csw-logging {
        | appenders = ["csw.services.logging.appenders.CustomAppenderBuilderObject$"]
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
    CustomAppenderBuilderObject.logBuffer.forall(log ⇒ log("IpAddress") == hostName) shouldBe true

    Await.result(actorSystem.terminate(), 10.seconds)
  }

  test("should be able to add and configure a custom appender using a class extending from CustomAppenderBuilder") {

    val config = ConfigFactory.parseString("""
        |csw-logging {
        | appenders = ["csw.services.logging.appenders.CustomAppenderBuilderClass"]
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
    customAppender.logBuffer.forall(log ⇒ log("IpAddress") == hostName) shouldBe true

    Await.result(actorSystem.terminate(), 10.seconds)
  }
}
