package csw.services.alarm.client.internal

import java.io.File

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}
import csw.services.alarm.api.exceptions.{
  InvalidSeverityException,
  KeyNotFoundException,
  NoAlarmsFoundException,
  ResetOperationNotAllowed
}
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.AcknowledgementStatus.{Acknowledged, UnAcknowledged}
import csw.services.alarm.api.models.ActivationStatus.{Active, Inactive}
import csw.services.alarm.api.models.AlarmSeverity.{Disconnected, Okay}
import csw.services.alarm.api.models.Key.{AlarmKey, GlobalKey}
import csw.services.alarm.api.models.LatchStatus.{Latched, UnLatched}
import csw.services.alarm.api.models.ShelveStatus.{Shelved, UnShelved}
import csw.services.alarm.api.models._
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmSubscription}
import csw.services.alarm.client.internal.configparser.ConfigParser
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutMessage.{CancelShelveTimeout, ScheduleShelveTimeout}
import io.lettuce.core.KeyValue
import reactor.core.publisher.FluxSink.OverflowStrategy
import romaine.{RedisAsyncScalaApi, RedisKeySpaceApi}

import scala.async.Async._
import scala.concurrent.Future

class AlarmServiceImpl(
    metadataApi: RedisAsyncScalaApi[MetadataKey, AlarmMetadata],
    severityApi: RedisAsyncScalaApi[SeverityKey, AlarmSeverity],
    statusApi: RedisAsyncScalaApi[StatusKey, AlarmStatus],
    statusStreamApiFactory: () ⇒ RedisKeySpaceApi[StatusKey, AlarmStatus],
    shelveTimeoutActorFactory: ShelveTimeoutActorFactory
)(implicit actorSystem: ActorSystem)
    extends AlarmAdminService {

  import actorSystem.dispatcher

  private lazy val shelveTimeoutRef = shelveTimeoutActorFactory.make(key ⇒ unShelve(key, cancelShelveTimeout = false))

  private val refreshInSeconds       = actorSystem.settings.config.getInt("alarm.refresh-in-seconds") // default value is 3 seconds
  private val maxMissedRefreshCounts = actorSystem.settings.config.getInt("alarm.max-missed-refresh-counts") //default value is 3 times
  private val ttlInSeconds           = refreshInSeconds * maxMissedRefreshCounts
  private val log                    = AlarmServiceLogger.getLogger

  implicit val mat: Materializer = ActorMaterializer()

  override def initAlarms(inputFile: File, reset: Boolean): Future[Unit] = async {
    log.debug(s"Initializing alarm store from file [${inputFile.getAbsolutePath}] with reset [$reset]")
    val inputConfig      = ConfigFactory.parseFile(inputFile).resolve(ConfigResolveOptions.noSystem())
    val alarmMetadataSet = ConfigParser.parseAlarmMetadataSet(inputConfig)

    if (reset) await(resetAlarmStore())

    await(setAlarmStore(alarmMetadataSet))
  }

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    log.debug(s"Setting severity [${severity.name}] for alarm [${key.value}] with expire timeout [$ttlInSeconds] seconds")

    // get alarm metadata
    val exception = KeyNotFoundException(key)
    log.error(exception.getMessage, ex = exception)
    val alarm = await(metadataApi.get(key)).getOrElse(throw exception)

    // validate if the provided severity is supported by this alarm
    if (!alarm.allSupportedSeverities.contains(severity)) {
      val exception = InvalidSeverityException(key, alarm.allSupportedSeverities, severity)
      log.error(exception.getMessage, ex = exception)
      throw exception
    }

    // get the current severity of the alarm
    val currentSeverity = await(severityApi.get(key)).getOrElse(Disconnected)

    // set the severity of the alarm so that it does not transition to `Disconnected` state
    await(severityApi.setex(key, ttlInSeconds, severity))

    // get alarm status
    val status    = await(statusApi.get(key)).getOrElse(AlarmStatus())
    var newStatus = status

    def shouldUpdateLatchStatus: Boolean                     = alarm.isLatchable && severity.isHighRisk
    def shouldUpdateLatchedSeverityWhenLatchable: Boolean    = shouldUpdateWhenLactched || shouldUpdateWhenUnLactched
    def shouldUpdateWhenLactched: Boolean                    = alarm.isLatchable && severity.isHighRisk && severity > status.latchedSeverity
    def shouldUpdateWhenUnLactched: Boolean                  = alarm.isLatchable && status.latchStatus == UnLatched && !severity.isHighRisk
    def shouldUpdateLatchedSeverityWhenNotLatchable: Boolean = !alarm.isLatchable

    if (shouldUpdateLatchStatus) newStatus = newStatus.copy(latchStatus = Latched)

    if (shouldUpdateLatchedSeverityWhenLatchable || shouldUpdateLatchedSeverityWhenNotLatchable)
      newStatus = newStatus.copy(latchedSeverity = severity)

    // derive acknowledgement status
    if (severity.isHighRisk && severity != currentSeverity) {
      if (alarm.isAutoAcknowledgeable) newStatus = newStatus.copy(acknowledgementStatus = Acknowledged)
      else newStatus = newStatus.copy(acknowledgementStatus = UnAcknowledged)
    }

    // update alarm status only when severity changes
    if (newStatus != status) await(statusApi.set(key, newStatus))
  }

  override def getSeverity(key: AlarmKey): Future[AlarmSeverity] = async {
    log.debug(s"Getting severity for alarm [${key.value}]")
    if (await(metadataApi.exists(key)))
      await(severityApi.get(key)).getOrElse(Disconnected)
    else {
      val exception = KeyNotFoundException(key)
      log.error(exception.getMessage, ex = exception)
      throw exception
    }
  }

  override def getStatus(key: AlarmKey): Future[AlarmStatus] = async {
    log.debug(s"Getting status for alarm [${key.value}]")
    await(statusApi.get(key)).getOrElse(throw KeyNotFoundException(key))
  }

  override def getMetadata(key: AlarmKey): Future[AlarmMetadata] = async {
    log.debug(s"Getting metadata for alarm [${key.value}]")
    await(metadataApi.get(key)).getOrElse(throw KeyNotFoundException(key))
  }

  override def getMetadata(key: Key): Future[List[AlarmMetadata]] = async {
    log.debug(s"Getting metadata for alarms matching [${key.value}]")
    val metadataKeys = await(metadataApi.keys(key))
    if (metadataKeys.isEmpty) throw NoAlarmsFoundException()
    await(metadataApi.mget(metadataKeys)).map(_.getValue)
  }

  override def acknowledge(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Acknowledge alarm [${key.value}]")
    if (await(metadataApi.exists(key))) {
      val status = await(statusApi.get(key)).getOrElse(AlarmStatus())

      if (status.acknowledgementStatus == UnAcknowledged) // save the set call if status is already Acknowledged
        await(statusApi.set(key, status.copy(acknowledgementStatus = Acknowledged)))
    } else throw KeyNotFoundException(key)
  }

  // reset is only called when severity is `Okay`
  override def reset(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Reset alarm [${key.value}]")
    if (await(metadataApi.exists(key))) {
      val currentSeverity = await(getSeverity(key))
      if (currentSeverity != Okay) throw ResetOperationNotAllowed(key, currentSeverity)

      val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
      if (status.acknowledgementStatus == Acknowledged || status.latchStatus == Latched || status.latchedSeverity != Okay) {
        val resetStatus = status.copy(acknowledgementStatus = UnAcknowledged, latchStatus = UnLatched, latchedSeverity = Okay)
        await(statusApi.set(key, resetStatus))
      }
    } else throw KeyNotFoundException(key)
  }

  override def shelve(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Shelve alarm [${key.value}]")
    val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
    if (status.shelveStatus != Shelved) {
      await(statusApi.set(key, status.copy(shelveStatus = Shelved)))
      shelveTimeoutRef ! ScheduleShelveTimeout(key) // start shelve timeout for this alarm (default 8 AM local time)
    }
  }

  // this will most likely be called when operator manually un-shelves an already shelved alarm
  override def unShelve(key: AlarmKey): Future[Unit] = unShelve(key, cancelShelveTimeout = true)

  private def unShelve(key: AlarmKey, cancelShelveTimeout: Boolean): Future[Unit] = async {
    log.debug(s"Un-shelve alarm [${key.value}]")
    //TODO: decide whether to  unshelve an alarm when it goes to okay
    val status = await(statusApi.get(key)).getOrElse(AlarmStatus())
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
    log.debug(s"Activate alarm [${key.value}]")
    val metadata = await(metadataApi.get(key)).getOrElse(throw KeyNotFoundException(key))
    if (!metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Active)))
  }

  override def deActivate(key: AlarmKey): Future[Unit] = async {
    log.debug(s"Deactivate alarm [${key.value}]")
    val metadata = await(metadataApi.get(key)).getOrElse(throw KeyNotFoundException(key))
    if (metadata.isActive) await(metadataApi.set(key, metadata.copy(activationStatus = Inactive)))
  }

  override def getAggregatedSeverity(key: Key): Future[AlarmSeverity] = async {
    log.debug(s"Get aggregated severity for alarm [${key.value}]")
    val statusKeys = await(statusApi.keys(key))
    if (statusKeys.isEmpty) throw NoAlarmsFoundException()

    val metadata = await(metadataApi.get(key)).getOrElse(throw KeyNotFoundException(key))

    val statusList = await(statusApi.mget(statusKeys))
    statusList
      .collect {
        case status: KeyValue[StatusKey, AlarmStatus] if metadata.isActive ⇒ status.getValue.latchedSeverity
      }
      .reduceRight((previous, current) ⇒ previous max current)
  }

  override def getAggregatedHealth(key: Key): Future[AlarmHealth] = {
    log.debug(s"Get aggregated health for alarm [${key.value}]")
    getAggregatedSeverity(key).map(AlarmHealth.fromSeverity)
  }

  override def subscribeAggregatedSeverityCallback(key: Key, callback: AlarmSeverity ⇒ Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated severity for alarm [${key.value}] with a callback")
    subscribeAggregatedSeverity(key)
      .to(Sink.foreach(callback))
      .run()
  }

  override def subscribeAggregatedHealthCallback(key: Key, callback: AlarmHealth ⇒ Unit): AlarmSubscription = {
    log.debug(s"Subscribe aggregated health for alarm [${key.value}] with a callback")
    subscribeAggregatedSeverity(key)
      .map(AlarmHealth.fromSeverity)
      .to(Sink.foreach(callback))
      .run()
  }

  override def subscribeAggregatedSeverityActorRef(key: Key, actorRef: ActorRef[AlarmSeverity]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated severity for alarm [${key.value}] with an actor")
    subscribeAggregatedSeverityCallback(key, actorRef ! _)
  }

  override def subscribeAggregatedHealthActorRef(key: Key, actorRef: ActorRef[AlarmHealth]): AlarmSubscription = {
    log.debug(s"Subscribe aggregated helath for alarm [${key.value}] with an actor")
    subscribeAggregatedHealthCallback(key, actorRef ! _)
  }

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

  private def setAlarmStore(alarmMetadataSet: AlarmMetadataSet) = {
    val alarms = alarmMetadataSet.alarms

    val metadataMap = alarms.map(metadata ⇒ MetadataKey.fromAlarmKey(metadata.alarmKey) → metadata).toMap
    val statusMap   = alarms.map(metadata ⇒ StatusKey.fromAlarmKey(metadata.alarmKey)   → AlarmStatus()).toMap

    Future.sequence(
      List(
        metadataApi.mset(metadataMap),
        statusApi.mset(statusMap)
      )
    )
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
