package csw.alarm
import akka.Done
import akka.actor.{typed, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config._
import csw.params.core.models.Subsystem.NFIRAOS
import csw.alarm.api.models.AlarmSeverity.Okay
import csw.alarm.api.models.Key.AlarmKey
import csw.alarm.api.models.{AlarmHealth, AlarmMetadata, AlarmStatus, FullAlarmSeverity}
import csw.alarm.client.AlarmServiceFactory
import csw.location.javadsl.JLocationServiceFactory
import csw.location.scaladsl.LocationServiceFactory

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object AlarmServiceClientExampleApp {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  private val locationService           = LocationServiceFactory.withSystem(actorSystem)
  private val jlocationService          = JLocationServiceFactory.withSystem(actorSystem)

  private def behaviour[T]: Behaviors.Receive[T] = Behaviors.receive { (ctx, msg) ⇒
    println(msg)
    Behaviors.same
  }

  val severityActorRef = typed.ActorSystem(behaviour[FullAlarmSeverity], "fullSeverityActor")
  val healthActorRef   = typed.ActorSystem(behaviour[AlarmHealth], "healthActor")

  //#create-scala-api
  // create alarm client using host and port of alarm server
  private val clientAPI1 = new AlarmServiceFactory().makeClientApi("localhost", 5225)

  // create alarm client using location service
  private val clientAPI2 = new AlarmServiceFactory().makeClientApi(locationService)

  // create alarm admin using host and port of alarm server
  private val adminAPI1 = new AlarmServiceFactory().makeAdminApi("localhost", 5226)

  // create alarm admin using location service
  private val adminAPI2 = new AlarmServiceFactory().makeAdminApi(locationService)
  //#create-scala-api

  //#create-java-api
  // create alarm client using host and port of alarm server
  private val jclientAPI1 = new AlarmServiceFactory().jMakeClientApi("localhost", 5227, actorSystem)

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

  //#acknowledge
  async {
    await(adminAPI1.acknowledge(alarmKey))
  }
  //#acknowledge

  //#shelve
  async {
    await(adminAPI1.shelve(alarmKey))
  }
  //#shelve

  //#unshelve
  async {
    await(adminAPI1.unshelve(alarmKey))
  }
  //#unshelve

  //#reset
  async {
    await(adminAPI1.reset(alarmKey))
  }
  //#reset

  //#getMetadata
  async {
    val metadata: AlarmMetadata = await(adminAPI1.getMetadata(alarmKey))
  }
  //#getMetadata

  //#getStatus
  async {
    val status: AlarmStatus = await(adminAPI1.getStatus(alarmKey))
  }
  //#getStatus

  //#getCurrentSeverity
  async {
    val severity: FullAlarmSeverity = await(adminAPI1.getCurrentSeverity(alarmKey))
  }
  //#getCurrentSeverity

  //#getAggregatedSeverity
  async {
    val aggregatedSeverity: FullAlarmSeverity = await(adminAPI1.getAggregatedSeverity(alarmKey))
  }
  //#getAggregatedSeverity

  //#getAggregatedHealth
  async {
    val health: AlarmHealth = await(adminAPI1.getAggregatedHealth(alarmKey))
  }
  //#getAggregatedHealth

  //#subscribeAggregatedSeverityCallback
  adminAPI1.subscribeAggregatedSeverityCallback(alarmKey, aggregatedSeverity ⇒ { /* do something*/ })
  //#subscribeAggregatedSeverityCallback

  //#subscribeAggregatedSeverityActorRef
  adminAPI1.subscribeAggregatedSeverityActorRef(alarmKey, severityActorRef)
  //#subscribeAggregatedSeverityActorRef

  //#subscribeAggregatedHealthCallback
  adminAPI1.subscribeAggregatedHealthCallback(alarmKey, aggregatedHealth ⇒ { /* do something*/ })
  //#subscribeAggregatedHealthCallback

  //#subscribeAggregatedHealthActorRef
  adminAPI1.subscribeAggregatedHealthActorRef(alarmKey, healthActorRef)
  //#subscribeAggregatedHealthActorRef
}
