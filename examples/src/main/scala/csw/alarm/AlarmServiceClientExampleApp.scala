package csw.alarm
import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{ActorSystem, typed}
import akka.stream.ActorMaterializer
import com.typesafe.config._
import csw.alarm.api.models.AlarmSeverity.Okay
import csw.alarm.api.models.Key.{AlarmKey, ComponentKey, SubsystemKey}
import csw.alarm.api.models.{AlarmHealth, AlarmMetadata, AlarmStatus, FullAlarmSeverity}
import csw.alarm.api.scaladsl.{AlarmAdminService, AlarmService, AlarmSubscription}
import csw.alarm.client.AlarmServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.core.models.Subsystem.{IRIS, NFIRAOS}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object AlarmServiceClientExampleApp {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  implicit val mat: ActorMaterializer   = ActorMaterializer()
  private val locationService           = HttpLocationServiceFactory.makeLocalClient

  private def behaviour[T]: Behaviors.Receive[T] = Behaviors.receive { (ctx, msg) ⇒
    println(msg)
    Behaviors.same
  }

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

  val clientAPI: AlarmService     = clientAPI1
  val adminAPI: AlarmAdminService = adminAPI1

  //#setSeverity-scala
  val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm")
  val resultF: Future[Done] = clientAPI.setSeverity(alarmKey, Okay)
  //#setSeverity-scala


  //#initAlarms
  val resource             = "test-alarms/valid-alarms.conf"
  val alarmsConfig: Config = ConfigFactory.parseResources(resource)
  val result2F: Future[Done] = adminAPI.initAlarms(alarmsConfig)
  //#initAlarms

  //#acknowledge
  val result3F: Future[Done] = adminAPI.acknowledge(alarmKey)
  //#acknowledge

  //#shelve
  val result4F: Future[Done] = adminAPI.shelve(alarmKey)
  //#shelve

  //#unshelve
  val result5F: Future[Done] = adminAPI.unshelve(alarmKey)
  //#unshelve

  //#reset
  val result6F: Future[Done] = adminAPI.reset(alarmKey))
  //#reset

  //#getMetadata
  val metadataF: Future[AlarmMetadata] = adminAPI.getMetadata(alarmKey)
  metadataF.onComplete {
    case Success(metadata) => println(s"${metadata.name}: ${metadata.description}")
    case Failure(exception) => println(s"Error getting metadata: ${exception.getMessage}")
  }
  //#getMetadata

  //#getStatus
  val statusF: Future[AlarmStatus] = adminAPI.getStatus(alarmKey)
  statusF.onComplete {
    case Success(status) => println(s"${status.alarmTime}: ${status.latchedSeverity}")
    case Failure(exception) => println(s"Error getting status: ${exception.getMessage}")
  }
  //#getStatus

  //#getCurrentSeverity
  val severityF: Future[FullAlarmSeverity] = adminAPI.getCurrentSeverity(alarmKey)
  severityF.onComplete {
    case Success(severity) => println(s"${severity.name}: ${severity.level}")
    case Failure(exception) => println(s"Error getting severity: ${exception.getMessage}")
  }
  //#getCurrentSeverity

  //#getAggregatedSeverity
  val componentKey                          = ComponentKey(NFIRAOS, "tromboneAssembly")
  val aggregatedSeverityF: Future[FullAlarmSeverity] = adminAPI.getAggregatedSeverity(componentKey)
  aggregatedSeverityF.onComplete {
    case Success(severity) => println(s"aggregate severity: ${severity.name}: ${severity.level}")
    case Failure(exception) => println(s"Error getting aggregate severity: ${exception.getMessage}")
  }
  //#getAggregatedSeverity

  //#getAggregatedHealth
  val subsystemKey        = SubsystemKey(IRIS)
  val healthF: Future[AlarmHealth] = adminAPI.getAggregatedHealth(subsystemKey)
  healthF.onComplete {
    case Success(health) => println(s"${subsystemKey.subsystem.name} health = ${health.entryName}")
    case Failure(exception) => println(s"Error getting health: ${exception.getMessage}")
  }
  //#getAggregatedHealth

  //#subscribeAggregatedSeverityCallback
  val alarmSubscription: AlarmSubscription = adminAPI.subscribeAggregatedSeverityCallback(
    ComponentKey(NFIRAOS, "tromboneAssembly"),
    aggregatedSeverity ⇒ { /* do something*/ }
  )
  // to unsubscribe:
  val unsubscribe1F: Future[Done] = alarmSubscription.unsubscribe()
  //#subscribeAggregatedSeverityCallback

  //#subscribeAggregatedSeverityActorRef
  val severityActorRef = typed.ActorSystem(behaviour[FullAlarmSeverity], "fullSeverityActor")
  val alarmSubscription2: AlarmSubscription = adminAPI.subscribeAggregatedSeverityActorRef(SubsystemKey(NFIRAOS), severityActorRef)

  // to unsubscribe:
  val unsubscribe2F: Future[Done] = alarmSubscription2.unsubscribe()
  //#subscribeAggregatedSeverityActorRef

  //#subscribeAggregatedHealthCallback
  val alarmSubscription3: AlarmSubscription = adminAPI.subscribeAggregatedHealthCallback(
    ComponentKey(IRIS, "ImagerDetectorAssembly"),
    aggregatedHealth ⇒ { /* do something*/ }
  )

  // to unsubscribe
  val unsubscribe3F: Future[Done] = alarmSubscription3.unsubscribe()
  //#subscribeAggregatedHealthCallback

  //#subscribeAggregatedHealthActorRef
  val healthActorRef = typed.ActorSystem(behaviour[AlarmHealth], "healthActor")
  val alarmSubscription4: AlarmSubscription = adminAPI.subscribeAggregatedHealthActorRef(SubsystemKey(IRIS), healthActorRef)

  // to unsubscribe
  val unsubscribe4F: Future[Done] = alarmSubscription4.unsubscribe()
  //#subscribeAggregatedHealthActorRef
}
