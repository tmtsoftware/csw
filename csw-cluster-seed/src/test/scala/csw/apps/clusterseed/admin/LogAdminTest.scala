package csw.apps.clusterseed.admin

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.admin.TromboneHcd._
import csw.apps.clusterseed.admin.http.HttpSupport
import csw.apps.clusterseed.utils.AdminLogTestSuite
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.logging.internal.LoggingLevels.{ERROR, Level, WARN}
import csw.services.logging.internal.{GetComponentLogMetadata, LoggingLevels, LoggingSystem, SetComponentLogLevel}
import csw.services.logging.models.{FilterSet, LogMetadata}
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

object TromboneHcdLogger extends ComponentLogger("tromboneHcd")

object TromboneHcd {
  def props(componentName: String, loggingSystem: LoggingSystem) = Props(new TromboneHcd(componentName, loggingSystem))

  case object LogTrace
  case object LogDebug
  case object LogInfo
  case object LogWarn
  case object LogError
  case object LogFatal
}

class TromboneHcd(componentName: String, loggingSystem: LoggingSystem) extends TromboneHcdLogger.Actor {

  def receive = {
    case LogTrace                    => log.trace("Level is trace")
    case LogDebug                    => log.debug("Level is debug")
    case LogInfo                     => log.info("Level is info")
    case LogWarn                     => log.warn("Level is warn")
    case LogError                    => log.error("Level is error")
    case LogFatal                    => log.fatal("Level is fatal")
    case SetComponentLogLevel(level) ⇒ loggingSystem.addFilter(componentName, level)
    case GetComponentLogMetadata     ⇒ sender ! loggingSystem.getLogMetadata
    case x: Any                      => log.error(Map("@reason" -> "Unexpected actor message", "message" -> x.toString))
  }
}

class LogAdminTest extends AdminLogTestSuite with HttpSupport {
  import adminWiring.actorRuntime._
  val componentName    = "tromboneHcd"
  val tromboneActorRef = actorSystem.actorOf(TromboneHcd.props(componentName, loggingSystem), name = "TromboneActor")
  val connection       = AkkaConnection(ComponentId(componentName, ComponentType.HCD))
  Await.result(adminWiring.locationService.register(AkkaRegistration(connection, tromboneActorRef)), 5.seconds)

  // DEOPSCSW-127: Runtime update for logging characteristics
  test("should able to get the current component log meta data") {

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(scheme = "http", host = ClusterAwareSettings.hostname, port = 7878,
      path = s"/admin/logging/${connection.name}/level")

    val getLogMetadataRequest   = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse1 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)
    val logMetadata1            = Await.result(Unmarshal(getLogMetadataResponse1).to[LogMetadata], 5.seconds)

    getLogMetadataResponse1.status shouldBe StatusCodes.OK

    val config     = ConfigFactory.load().getConfig("csw-logging")
    val logLevel   = Level(config.getString("logLevel"))
    val akkaLevel  = Level(config.getString("akkaLogLevel"))
    val slf4jLevel = Level(config.getString("slf4jLogLevel"))
    val filterSet  = FilterSet.from(config)

    logMetadata1 shouldBe LogMetadata(logLevel, akkaLevel, slf4jLevel, filterSet)

    // updating default and akka log level
    loggingSystem.setLevel(LoggingLevels.ERROR)
    loggingSystem.setAkkaLevel(LoggingLevels.WARN)

    // verify getLogMetadata http request gives updated log levels in response
    val getLogMetadataResponse2 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)
    val logMetadata2            = Await.result(Unmarshal(getLogMetadataResponse2).to[LogMetadata], 5.seconds)

    logMetadata2 shouldBe LogMetadata(ERROR, WARN, slf4jLevel, filterSet)

    // reset log levels to default
    loggingSystem.setLevel(logLevel)
    loggingSystem.setAkkaLevel(akkaLevel)
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  test("should able to set filter of the component dynamically through http end point") {

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
    val uri = Uri.from(scheme = "http", host = ClusterAwareSettings.hostname, port = 7878,
      path = s"/admin/logging/${connection.name}/level", queryString = Some("value=error"))

    val request  = HttpRequest(HttpMethods.POST, uri = uri)
    val response = Await.result(Http().singleRequest(request), 5.seconds)

    response.status shouldBe StatusCodes.OK

    sendLogMsgs()
    Thread.sleep(100)

    val groupByAfterFilter      = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val tromboneLogsAfterFilter = groupByAfterFilter.get("tromboneHcd").get

    tromboneLogsAfterFilter.size shouldBe 3

    tromboneLogsAfterFilter.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }
  }
}
