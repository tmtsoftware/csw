package csw.apps.clusterseed.admin

import akka.actor.{ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.typed
import akka.typed.scaladsl.adapter._
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.admin.TromboneHcdMessages._
import csw.apps.clusterseed.admin.http.HttpSupport
import csw.apps.clusterseed.utils.AdminLogTestSuite
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.AkkaRegistration
import csw.services.logging.internal.LoggingLevels.{ERROR, Level, WARN}
import csw.services.logging.internal._
import csw.services.logging.models.LogMetadata
import csw.services.logging.scaladsl.{ComponentLogger, LogAdminActor}

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

object TromboneHcdLogger extends ComponentLogger("tromboneHcd")

trait TromboneHcdMessages
object TromboneHcdMessages {
  case object LogTrace extends TromboneHcdMessages
  case object LogDebug extends TromboneHcdMessages
  case object LogInfo  extends TromboneHcdMessages
  case object LogWarn  extends TromboneHcdMessages
  case object LogError extends TromboneHcdMessages
  case object LogFatal extends TromboneHcdMessages
}
class TromboneHcd(componentName: String, loggingSystem: LoggingSystem) extends TromboneHcdLogger.Actor {

  def receive: PartialFunction[Any, Unit] = {
    case LogTrace                                          ⇒ log.trace("Level is trace")
    case LogDebug                                          ⇒ log.debug("Level is debug")
    case LogInfo                                           ⇒ log.info("Level is info")
    case LogWarn                                           ⇒ log.warn("Level is warn")
    case LogError                                          ⇒ log.error("Level is error")
    case LogFatal                                          ⇒ log.fatal("Level is fatal")
    case SetComponentLogLevel(`componentName`, level)      ⇒ loggingSystem.setComponentLogLevel(componentName, level)
    case GetComponentLogMetadata(`componentName`, replyTo) ⇒ replyTo ! loggingSystem.getLogMetadata(componentName)
    case x: Any                                            ⇒ log.error("Unexpected actor message", Map("message" -> x.toString))
  }
}

object TromboneHcd {
  def props(componentName: String, loggingSystem: LoggingSystem) = Props(new TromboneHcd(componentName, loggingSystem))
}

class LogAdminTest extends AdminLogTestSuite with HttpSupport {
  import adminWiring.actorRuntime._
  val compName = "tromboneHcd"
  val tromboneActorRef: ActorRef =
    actorSystem.actorOf(TromboneHcd.props(compName, loggingSystem), name = "TromboneActor")
  val tromboneAdminRef: typed.ActorRef[LogControlMessages] =
    actorSystem.spawn(LogAdminActor.behavior(loggingSystem), "LogAdminActor")
  val connection = AkkaConnection(ComponentId(compName, ComponentType.HCD))
  Await.result(adminWiring.locationService.register(AkkaRegistration(connection, tromboneActorRef, tromboneAdminRef)),
               5.seconds)

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-168: Actors can receive and handle runtime update for logging characteristics
  test("should able to get the current component log meta data") {

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(scheme = "http",
                                     host = ClusterAwareSettings.hostname,
                                     port = 7878,
                                     path = s"/admin/logging/${connection.name}/level")

    val getLogMetadataRequest   = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse1 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)
    val logMetadata1            = Await.result(Unmarshal(getLogMetadataResponse1).to[LogMetadata], 5.seconds)

    getLogMetadataResponse1.status shouldBe StatusCodes.OK

    val config            = ConfigFactory.load().getConfig("csw-logging")
    val logLevel          = Level(config.getString("logLevel"))
    val akkaLevel         = Level(config.getString("akkaLogLevel"))
    val slf4jLevel        = Level(config.getString("slf4jLogLevel"))
    val componentLogLevel = Level(config.getObject("component-log-levels").unwrapped().asScala(compName).toString)

    logMetadata1 shouldBe LogMetadata(logLevel, akkaLevel, slf4jLevel, componentLogLevel)

    // updating default and akka log level
    loggingSystem.setDefaultLogLevel(LoggingLevels.ERROR)
    loggingSystem.setAkkaLevel(LoggingLevels.WARN)

    // verify getLogMetadata http request gives updated log levels in response
    val getLogMetadataResponse2 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)
    val logMetadata2            = Await.result(Unmarshal(getLogMetadataResponse2).to[LogMetadata], 5.seconds)

    logMetadata2 shouldBe LogMetadata(ERROR, WARN, slf4jLevel, componentLogLevel)

    // reset log levels to default
    loggingSystem.setDefaultLogLevel(logLevel)
    loggingSystem.setAkkaLevel(akkaLevel)
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-168: Actors can receive and handle runtime update for logging characteristics
  test("should able to set log level of the component dynamically through http end point") {

    def sendLogMsgs(): Unit = {
      tromboneActorRef ! LogTrace
      tromboneActorRef ! LogDebug
      tromboneActorRef ! LogInfo
      tromboneActorRef ! LogWarn
      tromboneActorRef ! LogError
      tromboneActorRef ! LogFatal
      tromboneActorRef ! "Unknown"
    }

    sendLogMsgs()
    Thread.sleep(100)

    // default logging level for tromboneHcd is info
    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneHcdLogs          = groupByComponentNamesLog.get("tromboneHcd").get

    tromboneHcdLogs.size shouldBe 5

    tromboneHcdLogs.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true
    }

    logBuffer.clear()

    // set level of tromboneHcd to error through http endpoint
    val uri = Uri.from(scheme = "http",
                       host = ClusterAwareSettings.hostname,
                       port = 7878,
                       path = s"/admin/logging/${connection.name}/level",
                       queryString = Some("value=error"))

    val request  = HttpRequest(HttpMethods.POST, uri = uri)
    val response = Await.result(Http().singleRequest(request), 5.seconds)

    response.status shouldBe StatusCodes.OK

    sendLogMsgs()
    Thread.sleep(100)

    val groupByAfterFilter      = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneLogsAfterFilter = groupByAfterFilter("tromboneHcd")

    tromboneLogsAfterFilter.size shouldBe 3

    tromboneLogsAfterFilter.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }
  }

  test("should give appropriate exception when component name is incorrect") {
    // send http get metadata request for invalid component
    val getLogMetadataUri = Uri.from(scheme = "http",
                                     host = ClusterAwareSettings.hostname,
                                     port = 7878,
                                     path = s"/admin/logging/abcd-hcd-akka/level")

    val getLogMetadataRequest   = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse1 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)

    getLogMetadataResponse1.status shouldBe StatusCodes.NotFound
  }
}
