package csw.services.alarm.client

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.messages.location.javadsl.ILocationService
import csw.messages.location.scaladsl.LocationService
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.commons.serviceresolver.{
  AlarmServiceHostPortResolver,
  AlarmServiceLocationResolver,
  AlarmServiceResolver
}
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.{AlarmServiceImpl, JAlarmServiceImpl}
import io.lettuce.core.RedisClient
import romaine.RomaineFactory

import scala.concurrent.ExecutionContext

/**
 * Factory to create AlarmService
 */
class AlarmServiceFactory(redisClient: RedisClient = RedisClient.create()) {

  /**
   * A java helper to construct AlarmServiceFactory
   * @return
   */
  def this() = this(RedisClient.create())

  /**
   * Creates [[AlarmAdminService]] instance for admin users using [[LocationService]]
   *
   * @param locationService instance which will be used to resolve the location of alarm server
   * @param system an actor system required for underlying actors
   * @return an instance of [[AlarmAdminService]]
   */
  def makeAdminApi(locationService: LocationService)(implicit system: ActorSystem): AlarmAdminService = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceLocationResolver(locationService))
  }

  /**
   * Creates [[AlarmAdminService]] instance for admin users using host port of alarm server
   *
   * @param host of the alarm server
   * @param port on which alarm server is running
   * @param system an actor system required for underlying actors
   * @return an instance of [[AlarmAdminService]]
   */
  def makeAdminApi(host: String, port: Int)(implicit system: ActorSystem): AlarmAdminService = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceHostPortResolver(host, port))
  }

  /**
   * Creates [[AlarmService]] instance for non admin users using [[LocationService]]
   *
   * @param locationService instance which will be used to resolve the location of alarm server
   * @param system an actor system required for underlying actors
   * @return an instance of [[AlarmService]]
   */
  def makeClientApi(locationService: LocationService)(implicit system: ActorSystem): AlarmService =
    makeAdminApi(locationService)

  /**
   * Creates [[AlarmService]] instance for non admin users using host and port of alarm server
   *
   * @param host of the alarm server
   * @param port on which alarm server is running
   * @param system an actor system required for underlying actors
   * @return an instance of [[AlarmService]]
   */
  def makeClientApi(host: String, port: Int)(implicit system: ActorSystem): AlarmService = makeAdminApi(host, port)

  /**
   * Creates [[IAlarmService]] instance for non admin users using [[ILocationService]]
   *
   * @param locationService instance which will be used to resolve the location of alarm server
   * @param system an actor system required for underlying actors
   * @return an instance of [[IAlarmService]]
   */
  def jMakeClientApi(locationService: ILocationService, system: ActorSystem): IAlarmService =
    new JAlarmServiceImpl(makeAdminApi(locationService.asScala)(system))

  /**
   * Creates [[IAlarmService]] instance for non admin users using host and port of alarm server
   *
   * @param host of the alarm server
   * @param port on which alarm server is running
   * @param system an actor system required for underlying actors
   * @return an instance of [[IAlarmService]]
   */
  def jMakeClientApi(host: String, port: Int, system: ActorSystem): IAlarmService =
    new JAlarmServiceImpl(makeAdminApi(host, port)(system))

  /************ INTERNAL ************/
  private def alarmService(alarmServiceResolver: AlarmServiceResolver)(implicit system: ActorSystem, ec: ExecutionContext) = {
    val settings = new Settings(ConfigFactory.load())
    val redisConnectionsFactory =
      new RedisConnectionsFactory(alarmServiceResolver, settings.masterId, new RomaineFactory(redisClient))
    new AlarmServiceImpl(redisConnectionsFactory, settings)
  }

  private[alarm] def makeAlarmImpl(locationService: LocationService)(implicit system: ActorSystem) = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceLocationResolver(locationService))
  }
}
