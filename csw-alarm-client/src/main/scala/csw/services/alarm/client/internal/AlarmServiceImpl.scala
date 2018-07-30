package csw.services.alarm.client.internal

import java.io.File

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import csw.services.alarm.api.exceptions.{InvalidSeverityException, NoAlarmsFoundException, ResetOperationFailedException}
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.Okay
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models._
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmSubscription}
import csw.services.alarm.client.internal.configparser.ConfigParser
import csw.services.alarm.client.internal.redis.scala_wrapper.{RedisAsyncScalaApi, RedisStreamApi}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}
import io.lettuce.core.KeyValue
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.concurrent.Future

//why all apis are not call-by-name
class AlarmServiceImpl(
    metadataApi: RedisAsyncScalaApi[MetadataKey, AlarmMetadata],
    severityApi: RedisAsyncScalaApi[SeverityKey, AlarmSeverity],
    statusApi: RedisAsyncScalaApi[StatusKey, AlarmStatus],
    statusStreamApiFactory: () ⇒ RedisStreamApi[StatusKey, AlarmStatus],
    shelveTimeoutActorFactory: ShelveTimeoutActorFactory
)(implicit actorSystem: ActorSystem)
    extends AlarmAdminService {

  import actorSystem.dispatcher

  private lazy val shelveTimeoutRef = shelveTimeoutActorFactory.make(key ⇒ unShelve(key, cancelShelveTimeout = false))

  private val refreshInSeconds       = actorSystem.settings.config.getInt("alarm.refresh-in-seconds") // default value is 3 seconds
  private val maxMissedRefreshCounts = actorSystem.settings.config.getInt("alarm.max-missed-refresh-counts") //default value is 3 times
  private val ttlInSeconds           = refreshInSeconds * maxMissedRefreshCounts

  implicit val mat: Materializer = ActorMaterializer()

  override def initAlarms(inputFile: File, reset: Boolean): Future[Unit] = async {
    val inputConfig      = ConfigFactory.parseFile(inputFile).resolve(ConfigResolveOptions.noSystem())
    val alarmMetadataSet = ConfigParser.parseAlarmMetadataSet(inputConfig)

    if (reset) await(resetAlarmStore())

    await(setAlarmStore(alarmMetadataSet))
  }

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    // get alarm metadata
    val alarm = await(metadataApi.get(key))

    // validate if the provided severity is supported by this alarm
    if (!alarm.allSupportedSeverities.contains(severity))
      throw InvalidSeverityException(key, alarm.allSupportedSeverities, severity)

    // get the current severity of the alarm
    val currentSeverity = await(severityApi.get(key))

    // set the severity of the alarm so that it does not transition to `Disconnected` state
    await(severityApi.setex(key, ttlInSeconds, severity))

    // get alarm status
    var status        = await(statusApi.get(key))
    var statusChanged = false

    // derive latch status for latchable alarms
    if (alarm.isLatchable && severity.isHighRisk && severity > status.latchedSeverity) {
      status = status.copy(latchStatus = Latched, latchedSeverity = severity)
      statusChanged = true
    }

    // derive latch status for un-latchable alarms
    if (!alarm.isLatchable && severity != currentSeverity) {
      status = status.copy(latchedSeverity = severity)
      statusChanged = true
    }

    // derive acknowledgement status
    if (severity.isHighRisk && severity != currentSeverity) {
      if (alarm.isAutoAcknowledgeable) status = status.copy(acknowledgementStatus = Acknowledged)
      else status = status.copy(acknowledgementStatus = UnAcknowledged)
      statusChanged = true
    }

    // update alarm status only when severity changes
    if (statusChanged) await(statusApi.set(key, status))
  }

  override def getSeverity(key: AlarmKey): Future[AlarmSeverity] = severityApi.get(key)

  override def getStatus(key: AlarmKey): Future[AlarmStatus] = statusApi.get(key)

  override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = metadataApi.get(key)

  override def getMetadata(key: Key): Future[List[AlarmMetadata]] = async {
    val metadataKeys = await(metadataApi.keys(key))
    if (metadataKeys.isEmpty) throw NoAlarmsFoundException()
    await(metadataApi.mget(metadataKeys)).map(_.getValue)
  }

  override def acknowledge(key: AlarmKey): Future[Unit] = async {
    val status = await(statusApi.get(key))
    if (status.acknowledgementStatus == UnAcknowledged) // save the set call if status is already Acknowledged
      await(statusApi.set(key, status.copy(acknowledgementStatus = Acknowledged)))
  }

  // reset is only called when severity is `Okay`
  override def reset(key: AlarmKey): Future[Unit] = async {
    val currentSeverity = await(severityApi.get(key))

    if (currentSeverity != Okay) throw ResetOperationFailedException(key, currentSeverity)

    val status = await(statusApi.get(key))
    if (status.acknowledgementStatus == UnAcknowledged || status.latchStatus == Latched || status.latchedSeverity != Okay) {
      val resetStatus = status.copy(acknowledgementStatus = Acknowledged, latchStatus = UnLatched, latchedSeverity = Okay)
      await(statusApi.set(key, resetStatus))
    }
  }

  override def shelve(key: AlarmKey): Future[Unit] = async {
    val status = await(statusApi.get(key))
    if (status.shelveStatus != Shelved) {
      await(statusApi.set(key, status.copy(shelveStatus = Shelved)))
      shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
    }
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  override def unShelve(key: AlarmKey): Future[Unit] = unShelve(key, cancelShelveTimeout = true)

  private def unShelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    //TODO: decide whether to unshelve an alarm when it goes to okay
    val status = await(statusApi.get(key))
    if (status.shelveStatus != UnShelved) {
      await(statusApi.set(key, status.copy(shelveStatus = UnShelved)))
      // if in case of manual un-shelve operation, cancel the scheduled timer for this alarm
      // this method is also called when scheduled timer for shelving of an alarm goes off (i.e. default 8 AM local time) with
      // cancelShelveTimeout as false
      // so, at this time `CancelShelveTimeout` should not be sent to `shelveTimeoutRef` as it is already cancelled
      if (cancelShelveTimeout) shelveTimeoutRef ! CancelShelveTimeout(key)
    }
  }

  override def activate(key: AlarmKey): Future[Unit] = async {
    val metadata = await(metadataApi.get(key))
    if (!metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Active)))
  }

  override def deActivate(key: AlarmKey): Future[Unit] = async {
    val metadata = await(metadataApi.get(key))
    if (metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Inactive)))
  }

  override def getAggregatedSeverity(key: Key): Future[AlarmSeverity] = async {
    val statusKeys = await(statusApi.keys(key))
    val metadata   = await(metadataApi.get(key))

    if (statusKeys.isEmpty) throw NoAlarmsFoundException()

    val statusList = await(statusApi.mget(statusKeys))
    statusList
      .collect {
        case status: KeyValue[StatusKey, AlarmStatus] if metadata.isActive ⇒ status.getValue.latchedSeverity
      }
      .reduceRight((previous, current) ⇒ previous max current)
  }

  override def getAggregatedHealth(key: Key): Future[AlarmHealth] = getAggregatedSeverity(key).map(AlarmHealth.fromSeverity)

  override def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription =
    subscribeAggregatedSeverity(key)
      .to(Sink.foreach(callback))
      .run()

  override def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription =
    subscribeAggregatedSeverity(key)
      .map(AlarmHealth.fromSeverity)
      .to(Sink.foreach(callback))
      .run()

  override def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription =
    subscribeAggregatedSeverityCallback(key, actorRef ! _)

  override def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription =
    subscribeAggregatedHealthCallback(key, actorRef ! _)

  // PatternMessage gives three values:
  // pattern: e.g  __keyspace@0__:status.nfiraos.*.*,
  // channel: e.g. __keyspace@0__:status.nfiraos.trombone.tromboneAxisLowLimitAlarm,
  // message: event type as value: e.g. set, expire, expired
  def subscribeAggregatedSeverity(key: Key): Source[AlarmSeverity, AlarmSubscription] = {
    val redisStreamApi     = statusStreamApiFactory() // create new connection for every client
    val keys: List[String] = List(StatusKey.fromAlarmKey(key).value)

    redisStreamApi
      .watchKeyspaceFieldAggregation[AlarmSeverity](keys, OverflowStrategy.LATEST, _.latchedSeverity, _.maxBy(_.level))
      .mapMaterializedValue { mat =>
        new AlarmSubscription {
          override def unsubscribe(): Future[Unit] = mat.unsubscribe()
          override def ready(): Future[Unit]       = mat.ready()
        }
      }
  }

  private def setAlarmStore(alarmMetadataSet: AlarmMetadataSet) =
    Future
      .traverse(alarmMetadataSet.alarms) { alarmMetadata ⇒
        val alarmKey = alarmMetadata.alarmKey
        metadataApi.set(alarmKey, alarmMetadata)
        statusApi.set(alarmKey, AlarmStatus())
        severityApi.set(alarmKey, AlarmSeverity.Disconnected)
      }

  private def resetAlarmStore() =
    Future
      .sequence(
        List(
          metadataApi.pdel(GlobalKey),
          statusApi.pdel(GlobalKey),
          severityApi.pdel(GlobalKey)
        )
      )
}
