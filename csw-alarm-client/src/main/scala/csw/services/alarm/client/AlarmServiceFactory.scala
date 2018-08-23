package csw.services.alarm.client

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.alarm.client.internal.commons.Settings
import csw.services.alarm.client.internal.commons.serviceresolver.{
  AlarmServiceHostPortResolver,
  AlarmServiceLocationResolver,
  AlarmServiceResolver
}
import csw.services.alarm.client.internal.redis.RedisConnectionsFactory
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.{AlarmServiceImpl, JAlarmServiceImpl}
import csw.services.location.javadsl.ILocationService
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import romaine.RomaineFactory

import scala.concurrent.ExecutionContext

class AlarmServiceFactory(redisClient: RedisClient = RedisClient.create()) {

  def this() = this(RedisClient.create())

  def makeAdminApi(locationService: LocationService)(implicit system: ActorSystem): AlarmServiceImpl = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceLocationResolver(locationService))
  }

  def makeAdminApi(host: String, port: Int)(implicit system: ActorSystem): AlarmServiceImpl = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceHostPortResolver(host, port))
  }

  def makeClientApi(locationService: LocationService)(implicit system: ActorSystem): AlarmServiceImpl =
    makeAdminApi(locationService)

  def makeClientApi(host: String, port: Int)(implicit system: ActorSystem): AlarmServiceImpl = makeAdminApi(host, port)

  def jMakeClientApi(locationService: ILocationService, system: ActorSystem): JAlarmServiceImpl = {
    new JAlarmServiceImpl(makeAdminApi(locationService.asScala)(system))
  }

  def jMakeClientApi(host: String, port: Int, system: ActorSystem): JAlarmServiceImpl = {
    new JAlarmServiceImpl(makeAdminApi(host, port)(system))
  }

  /************ INTERNAL ************/
  private def alarmService(alarmServiceResolver: AlarmServiceResolver)(implicit system: ActorSystem, ec: ExecutionContext) = {
    val settings = new Settings(ConfigFactory.load())
    val redisConnectionsFactory =
      new RedisConnectionsFactory(alarmServiceResolver, settings.masterId, new RomaineFactory(redisClient))
    val shelveTimeoutActorFactory = new ShelveTimeoutActorFactory()
    new AlarmServiceImpl(redisConnectionsFactory, shelveTimeoutActorFactory, settings)
  }
}
