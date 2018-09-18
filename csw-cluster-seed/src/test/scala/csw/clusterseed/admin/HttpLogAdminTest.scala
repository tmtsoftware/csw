package csw.clusterseed.admin

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.testkit.typed.TestKitSettings
import com.typesafe.config.ConfigFactory
import csw.clusterseed.admin.http.HttpSupport
import csw.clusterseed.internal.AdminWiring
import csw.clusterseed.utils.AdminLogTestSuite
import csw.config.server.commons.{ConfigServiceConnection, TestFileUtils}
import csw.config.server.{ServerWiring, Settings}
import csw.location.api.commons.ClusterAwareSettings
import csw.logging.internal.LoggingLevels.{ERROR, Level, WARN}
import csw.logging.internal._
import csw.logging.models.LogMetadata
import csw.logging.scaladsl.LoggingSystemFactory

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class HttpLogAdminTest extends AdminLogTestSuite with HttpSupport {

  private val adminWiring: AdminWiring = AdminWiring.make(Some(3653), Some(7888))
  import adminWiring.actorRuntime._

  implicit val typedSystem: ActorSystem[Nothing] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val serverWiring = ServerWiring.make(adminWiring.locationService)
  serverWiring.svnRepo.initSvnRepo()
  Await.result(serverWiring.httpService.registeredLazyBinding, 20.seconds)

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  private var loggingSystem: LoggingSystem = _

  override protected def beforeAll(): Unit = {
    loggingSystem = LoggingSystemFactory.start("logging", "version", hostName, adminWiring.actorSystem)
    loggingSystem.setAppenders(List(testAppender))

    logBuffer.clear()
    Await.result(adminWiring.adminHttpService.registeredLazyBinding, 5.seconds)
    // this will start seed on port 3653 and log admin server on 7888
    adminWiring.locationService
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = {
    testFileUtils.deleteServerFiles()
    Await.result(adminWiring.actorRuntime.shutdown(UnknownReason), 10.seconds)
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-160: Config(HTTP) Service can receive and handle runtime update for logging characteristics
  test("should able to get the component log meta data and set log level for http service") {

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = ClusterAwareSettings.hostname,
      port = 7888,
      path = s"/admin/logging/${ConfigServiceConnection.value.name}/level"
    )

    val getLogMetadataRequest   = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse1 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)
    val logMetadata1            = Await.result(Unmarshal(getLogMetadataResponse1).to[LogMetadata], 5.seconds)

    getLogMetadataResponse1.status shouldBe StatusCodes.OK

    val config     = ConfigFactory.load().getConfig("csw-logging")
    val logLevel   = Level(config.getString("logLevel"))
    val akkaLevel  = Level(config.getString("akkaLogLevel"))
    val slf4jLevel = Level(config.getString("slf4jLogLevel"))
    val componentLogLevel = Level(
      config
        .getObject("component-log-levels")
        .unwrapped()
        .asScala(ConfigServiceConnection.value.componentId.name)
        .toString
    )

    logMetadata1 shouldBe LogMetadata(logLevel, akkaLevel, slf4jLevel, componentLogLevel)

    // updating default and akka log level
    loggingSystem.setDefaultLogLevel(LoggingLevels.ERROR)
    loggingSystem.setAkkaLevel(LoggingLevels.WARN)

    // set log level of config service to error through http endpoint
    val setLogLevelUri = Uri.from(
      scheme = "http",
      host = ClusterAwareSettings.hostname,
      port = 7888,
      path = s"/admin/logging/${ConfigServiceConnection.value.name}/level",
      queryString = Some("value=error")
    )

    val request  = HttpRequest(HttpMethods.POST, uri = setLogLevelUri)
    val response = Await.result(Http().singleRequest(request), 5.seconds)

    response.status shouldBe StatusCodes.OK

    // verify getLogMetadata http request gives updated log levels in response
    val getLogMetadataResponse2 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)
    val logMetadata2            = Await.result(Unmarshal(getLogMetadataResponse2).to[LogMetadata], 5.seconds)

    getLogMetadataResponse2.status shouldBe StatusCodes.OK

    logMetadata2 shouldBe LogMetadata(ERROR, WARN, slf4jLevel, ERROR)

  }
}
