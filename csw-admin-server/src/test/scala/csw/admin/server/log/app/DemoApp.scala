package csw.admin.server.log.app

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory
import csw.admin.server.wiring.AdminWiring
import csw.command.client.messages.CommandMessage.Oneway
import csw.command.client.messages.ContainerCommonMessage.GetComponents
import csw.command.client.messages.ContainerMessage
import csw.command.client.models.framework.{Component, Components, ContainerLifecycleState}
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.client.ActorSystemFactory
import csw.location.server.internal.ServerWiring
import csw.logging.core.appenders.StdOutAppender
import csw.logging.core.scaladsl.{LoggerFactory, LoggingSystemFactory}
import csw.network.utils.Networks
import csw.params.commands.CommandResponse.OnewayResponse
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.Prefix

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AppLogger extends LoggerFactory("app")

/*
  This app is for testing runtime changes of component log level

  DemoApp does four things :
    1. Start seed node on port 3552
    2. Start AdminHttpServer on port 7878
    3. Creates a Laser Container with 3 components within it
    4. Laser Assembly start logging messages at all levels in infinite loop

  How to test :
    1. Start the app
    2. Import postman collection present under tools/postman which has two routes inside log admin folder (1. get log metadata 2. set log level)
    3. get log metadata :=> this will give current configuration for specified component
    4. set log level :=> update component name and value and verify on the console that logs are printed as per the updated log level.

  Important :
    Make sure you stop the app once you finish testing as it will not terminate automatically.

 */

object DemoApp extends App {

  val adminWiring: AdminWiring = new AdminWiring()

  val frameworkSystem = ActorSystemFactory.remote("framework")
  val frameworkWiring = FrameworkWiring.make(frameworkSystem)

  implicit val typedSystem: ActorSystem[Nothing] = frameworkWiring.actorSystem.toTyped
  implicit val testKitSettings: TestKitSettings  = TestKitSettings(typedSystem)

  private val cmdResponseProbe = TestProbe[OnewayResponse]
  private val startLoggingCmd  = CommandName("StartLogging")
  private val prefix           = Prefix("iris.command")

  private def startSeed() = {
    import adminWiring.actorRuntime._
    val loggingSystem = LoggingSystemFactory.start("logging", "version", Networks().hostname, actorSystem)
    loggingSystem.setAppenders(List(StdOutAppender))
    Await.result(ServerWiring.make(Some(3552)).locationHttpService.start(), 5.seconds)
    Await.result(adminWiring.adminHttpService.registeredLazyBinding, 5.seconds)
  }

  private def spawnContainer(): ActorRef[ContainerMessage] = {

    val config       = ConfigFactory.load("laser_container.conf")
    val containerRef = Await.result(Container.spawn(config, frameworkWiring), 5.seconds)

    val containerStateProbe = TestProbe[ContainerLifecycleState]
    assertThatContainerIsRunning(containerRef, containerStateProbe, 5.seconds)
    containerRef
  }

  startSeed()

  private val containerRef: ActorRef[ContainerMessage] = spawnContainer()

  val probe = TestProbe[Components]
  containerRef ! GetComponents(probe.ref)
  val components = probe.expectMessageType[Components].components

  private val laserComponent: Component = components.find(x ⇒ x.info.name.equals("Laser")).get

  while (true) {
    println("------------------------------------")
    laserComponent.supervisor ! Oneway(Setup(prefix, startLoggingCmd, None), cmdResponseProbe.ref)
    println("------------------------------------")
    Thread.sleep(1000)
  }
}
