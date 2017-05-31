package csw.apps.clusterseed.app

import akka.actor.{Actor, Props}
import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.logging.internal.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.services.logging.scaladsl.ComponentLogger

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AppLogger extends ComponentLogger("app")

/*
  This app is for testing runtime changes of component filter level

  DemoApp does four things :
    1. Start seed node on port 3552
    2. Start AdminHttpServer on port 7878
    3. Register component having name = `app` with location service
    4. Start logging messages at all levels in infinite loop

  How to test :
    1. Start the app
    2. Import postman collection present under tools/postman which has two routes inside log admin folder (1. get log metadata 2. set log level)
    3. get log metadata :=> this will give current configuration for specified component
    4. set log level :=> update component name and value and verify on the console that logs are printed as per the updated filter.

  Important :
    Make sure you stop the app once you finish testing as it will not terminate automatically.

 */

class DemoApp extends AppLogger.Simple {

  private val actorSystem = ClusterAwareSettings.onPort(3552).system
  val adminWiring         = new AdminWiring(actorSystem)
  import adminWiring._

  private val loggingSystem = actorRuntime.startLogging()

  def run(): Unit = {
    startSeed()
    registerWithLocationService()
    startLogging()
  }

  def startSeed(): Unit = {
    Await.result(adminHttpService.registeredLazyBinding, 5.seconds)
  }

  def registerWithLocationService(): Unit = {
    val componentName = "app"
    val connection    = AkkaConnection(ComponentId(componentName, ComponentType.HCD))
    val actorRef = actorSystem.actorOf(
      Props(new Actor {
        override def receive: Receive = {
          case SetComponentLogLevel(level) ⇒ loggingSystem.addFilter(componentName, level)
          case GetComponentLogMetadata     ⇒ sender ! loggingSystem.getLogMetadata()
          case unknown                     ⇒ log.error(s"Unknown message received => $unknown")
        }
      }),
      "test-actor"
    )
    val akkaRegistration = AkkaRegistration(connection, actorRef)

    log.info(s"Registering akka connection = ${connection.name} with location service")
    Await.result(locationService.register(akkaRegistration), 5.seconds)

    val result = Await.result(locationService.list, 5.seconds)
    log.debug(s"List of registered locations : $result")
  }

  def startLogging(): Unit = {
    while (true) {
      println("------------------------------------")
      log.trace("logging at trace level")
      log.debug("logging at debug level")
      log.info("logging at info level")
      log.warn("logging at warn level")
      log.error("logging at error level")
      log.fatal("logging at fatal level")
      println("------------------------------------")
      Thread.sleep(1000)
    }
  }
}

object DemoApp extends App {
  new DemoApp().run()
}
