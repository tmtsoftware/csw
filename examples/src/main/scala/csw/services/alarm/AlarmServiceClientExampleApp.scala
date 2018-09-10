package csw.services.alarm
import akka.Done
import akka.actor.ActorSystem
import com.typesafe.config._
import csw.messages.params.models.Subsystem.NFIRAOS
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.Key.AlarmKey
import csw.services.alarm.client.AlarmServiceFactory
import csw.services.location.javadsl.JLocationServiceFactory
import csw.services.location.scaladsl.LocationServiceFactory

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object AlarmServiceClientExampleApp {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  private val locationService           = LocationServiceFactory.withSystem(actorSystem)
  private val jlocationService          = JLocationServiceFactory.withSystem(actorSystem)

  //#create-scala-api
  // create alarm client using host and port of alarm server
  private val clientAPI1 = new AlarmServiceFactory().makeClientApi("localhost", 5225)

  // create alarm client using location service
  private val clientAPI2 = new AlarmServiceFactory().makeClientApi(locationService)

  // create alarm admin using host and port of alarm server
  private val adminAPI1 = new AlarmServiceFactory().makeAdminApi("localhost", 5225)

  // create alarm admin using location service
  private val adminAPI2 = new AlarmServiceFactory().makeAdminApi(locationService)
  //#create-scala-api

  //#create-java-api
  // create alarm client using host and port of alarm server
  private val jclientAPI1 = new AlarmServiceFactory().jMakeClientApi("localhost", 5225, actorSystem)

  // create alarm client using location service
  private val jclientAPI2 = new AlarmServiceFactory().jMakeClientApi(jlocationService, actorSystem)
  //#create-java-api

  val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm")

  val foo: Future[Done] =
    //#setSeverity-scala
    async {
      await(clientAPI1.setSeverity(alarmKey, Okay))
    }
  //#setSeverity-scala

  //#setSeverity-java
  private val done: Done = jclientAPI1.setSeverity(alarmKey, Okay).get()
  //#setSeverity-java

  //#initAlarms
  async {
    val alarmsConfig: Config = ConfigFactory.parseResources("test-alarms/valid-alarms.conf")
    await(adminAPI1.initAlarms(alarmsConfig))
  }
  //#initAlarms
}
