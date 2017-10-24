package csw.apps.clusterseed.admin

import akka.actor
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.apps.clusterseed.admin.http.HttpSupport
import csw.apps.clusterseed.components.StartLogging
import csw.apps.clusterseed.utils.AdminLogTestSuite
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.messages.ContainerCommonMessage.GetComponents
import csw.messages.framework.ContainerLifecycleState
import csw.messages.location.ComponentId
import csw.messages.location.ComponentType.{Assembly, HCD}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.{Component, Components, ContainerMessage}
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings}
import csw.services.logging.internal.LoggingLevels.{ERROR, Level, WARN}
import csw.services.logging.internal._
import csw.services.logging.models.LogMetadata

import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class AkkaLogAdminTest extends AdminLogTestSuite with HttpSupport {

  import adminWiring.actorRuntime._

  implicit val typedSystem: ActorSystem[Nothing] = actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val laserConnection            = AkkaConnection(ComponentId("Laser", Assembly))
  private val motionControllerConnection = AkkaConnection(ComponentId("Motion_Controller", HCD))
  private val galilConnection            = AkkaConnection(ComponentId("Galil", Assembly))

  private var containerActorSystem: actor.ActorSystem = _

  private var laserComponent: Component = _
  private var galilComponent: Component = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    // this will start seed on port 3552 and log admin server on 7878
    adminWiring.locationService

    containerActorSystem = ClusterSettings().joinLocal(3552).system

    // this will start container on random port and join seed and form a cluster
    val containerRef = startContainerAndWaitForRunning()
    extractComponentsFromContainer(containerRef)
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    Await.result(containerActorSystem.terminate(), 5.seconds)
  }

  def startContainerAndWaitForRunning(): ActorRef[ContainerMessage] = {
    val frameworkWiring = FrameworkWiring.make(containerActorSystem)
    val config          = ConfigFactory.load("laser_container.conf")
    val containerRef    = Await.result(Container.spawn(config, frameworkWiring), 5.seconds)

    val containerStateProbe = TestProbe[ContainerLifecycleState]
    assertThatContainerIsRunning(containerRef, containerStateProbe, 5.seconds)
    containerRef
  }

  def extractComponentsFromContainer(containerRef: ActorRef[ContainerMessage]): Unit = {
    val probe = TestProbe[Components]
    containerRef ! GetComponents(probe.ref)
    val components = probe.expectMsgType[Components].components

    laserComponent = components.find(x ⇒ x.info.name.equals("Laser")).get
    galilComponent = components.find(x ⇒ x.info.name.equals("Galil")).get
  }

  // DEOPSCSW-127: Runtime update for logging characteristics
  // DEOPSCSW-168: Actors can receive and handle runtime update for logging characteristics
  test("should able to get the current component log meta data") {

    // send http get metadata request and verify the response has correct log levels
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = ClusterAwareSettings.hostname,
      port = 7878,
      path = s"/admin/logging/${motionControllerConnection.name}/level"
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
      config.getObject("component-log-levels").unwrapped().asScala(motionControllerConnection.componentId.name).toString
    )

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

    laserComponent.supervisor ! StartLogging()
    Thread.sleep(100)

    // default logging level for Laser component is info
    val groupByComponentNamesLog = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val laserComponentLogs       = groupByComponentNamesLog(laserComponent.info.name)

    laserComponentLogs.exists(log ⇒ log("@severity").toString.toLowerCase.equalsIgnoreCase("info")) shouldBe true
    laserComponentLogs.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true
    }

    // set log level of laser component to error through http endpoint
    val uri = Uri.from(
      scheme = "http",
      host = ClusterAwareSettings.hostname,
      port = 7878,
      path = s"/admin/logging/${laserConnection.name}/level",
      queryString = Some("value=error")
    )

    val request  = HttpRequest(HttpMethods.POST, uri = uri)
    val response = Await.result(Http().singleRequest(request), 5.seconds)

    response.status shouldBe StatusCodes.OK

    Thread.sleep(100)
    logBuffer.clear()

    // laser and galil components, start logging messages at all log levels
    // and expected is that, laser component logs messages at and above Error level
    // and galil component  still logs messages at and above Info level
    laserComponent.supervisor ! StartLogging()
    galilComponent.supervisor ! StartLogging()
    Thread.sleep(100)

    val groupByAfterFilter       = logBuffer.groupBy(json ⇒ json("@componentName").toString)
    val laserCompLogsAfterFilter = groupByAfterFilter(laserConnection.componentId.name)
    val galilCompLogsAfterFilter = groupByAfterFilter(galilConnection.componentId.name)

    laserCompLogsAfterFilter.exists(log ⇒ log("@severity").toString.toLowerCase.equalsIgnoreCase("error")) shouldBe true
    laserCompLogsAfterFilter.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.ERROR shouldBe true
    }

    // this makes sure that, changing log level of one component (laser component) from container does not affect other components (galil component) log level
    galilCompLogsAfterFilter.exists(log ⇒ log("@severity").toString.toLowerCase.equalsIgnoreCase("info")) shouldBe true

    galilCompLogsAfterFilter.foreach { log ⇒
      val currentLogLevel = log("@severity").toString.toLowerCase
      Level(currentLogLevel) >= LoggingLevels.INFO shouldBe true
    }
  }

  test("should give appropriate exception when component name is incorrect") {
    // send http get metadata request for invalid component
    val getLogMetadataUri = Uri.from(
      scheme = "http",
      host = ClusterAwareSettings.hostname,
      port = 7878,
      path = s"/admin/logging/abcd-hcd-akka/level"
    )

    val getLogMetadataRequest   = HttpRequest(HttpMethods.GET, uri = getLogMetadataUri)
    val getLogMetadataResponse1 = Await.result(Http().singleRequest(getLogMetadataRequest), 5.seconds)

    getLogMetadataResponse1.status shouldBe StatusCodes.NotFound
  }

  test("should give appropriate exception when logging level is incorrect") {
    // send http set request for invalid log level
    val uri = Uri.from(
      scheme = "http",
      host = ClusterAwareSettings.hostname,
      port = 7878,
      path = s"/admin/logging/${laserConnection.name}/level",
      queryString = Some("value=error1")
    )

    val request  = HttpRequest(HttpMethods.POST, uri = uri)
    val response = Await.result(Http().singleRequest(request), 5.seconds)

    response.status shouldBe StatusCodes.BadRequest

  }
}
