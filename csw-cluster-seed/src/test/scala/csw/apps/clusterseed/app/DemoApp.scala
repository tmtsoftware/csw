package csw.apps.clusterseed.app

import akka.actor.{Actor, Props}
import akka.typed.scaladsl.adapter._
import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.AkkaRegistration
import csw.services.logging.internal.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.services.logging.scaladsl.{ComponentLogger, LogAdminActor}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AppLogger extends ComponentLogger("app")

/*
  This app is for testing runtime changes of component log level

  DemoApp does four things :
    1. Start seed node on port 3552
    2. Start AdminHttpServer on port 7878
    3. Register component having name = `app` with location service
    4. Start logging messages at all levels in infinite loop

  How to test :
    1. Start the app
    2. Import postman collection present under tools/postman which has two routes inside log admin folder (1. get log metadata 2. set log level)
    3. get log metadata :=> this will give current configuration for specified component
    4. set log level :=> update component name and value and verify on the console that logs are printed as per the updated log level.

  Important :
    Make sure you stop the app once you finish testing as it will not terminate automatically.

 */

class DemoApp extends AppLogger.Simple {

  val adminWiring = AdminWiring.make(ClusterAwareSettings.onPort(3552), Some(7878))
  import adminWiring._

  private val loggingSystem = actorRuntime.startLogging()

  def run(): Unit = {
    startSeed()
    registerWithLocationService()
    sampleLogging()
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
          case SetComponentLogLevel(name, level) ⇒
            loggingSystem.setComponentLogLevel(name, level)
          case GetComponentLogMetadata(name, replyTo) ⇒ {
            println("getMetadata")
            println(name)
            replyTo ! loggingSystem.getLogMetadata(name)
          }
          case unknown ⇒ log.error(s"Unknown message received => $unknown")
        }
      }),
      "test-actor"
    )

    val adminActorRef    = actorSystem.spawn(LogAdminActor.behavior(), "log-admin")
    val akkaRegistration = AkkaRegistration(connection, actorRef, adminActorRef)

    log.info(s"Registering akka connection = ${connection.name} with location service")
    Await.result(locationService.register(akkaRegistration), 5.seconds)

    val result = Await.result(locationService.list, 5.seconds)
    log.debug(s"List of registered locations : $result")
  }

  def sampleLogging(): Unit = {
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
