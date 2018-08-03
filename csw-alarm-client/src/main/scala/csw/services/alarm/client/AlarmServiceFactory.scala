package csw.services.alarm.client

import java.net.URI

import akka.actor.ActorSystem
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.commons.AlarmServiceLocationResolver
import csw.services.alarm.client.internal.redis.{RedisConnectionsFactory, RedisKeySpaceApiFactory}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.alarm.client.internal.{AlarmServiceImpl, JAlarmServiceImpl}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class AlarmServiceFactory(redisClient: RedisClient = RedisClient.create()) {

  def this() = this(RedisClient.create())

  def adminApi(locationService: LocationService)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext
  ): Future[AlarmAdminService] = async {
    val alarmURI = await(AlarmServiceLocationResolver.resolveWith(locationService))
    await(alarmService(alarmURI))
  }

  def adminApi(host: String, port: Int)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext
  ): Future[AlarmAdminService] = async {
    val alarmURI = await(AlarmServiceLocationResolver.resolveWith(host, port))
    await(alarmService(alarmURI))
  }

  def clientApi(locationService: LocationService)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext
  ): Future[AlarmService] = adminApi(locationService)

  def clientApi(host: String, port: Int)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext
  ): Future[AlarmService] = adminApi(host, port)

  def jClientApi(locationService: LocationService, actorSystem: ActorSystem): Future[IAlarmService] = {
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    adminApi(locationService)(actorSystem, ec).map(new JAlarmServiceImpl(_))
  }

  def jClientApi(host: String, port: Int, actorSystem: ActorSystem): Future[IAlarmService] = {
    implicit val ec: ExecutionContext = actorSystem.dispatcher
    adminApi(host, port)(actorSystem, ec).map(new JAlarmServiceImpl(_))
  }

  /************ INTERNAL ************/
  private def alarmService(alarmURI: URI)(implicit actorSystem: ActorSystem, ec: ExecutionContext) = async {
    val masterId                = actorSystem.settings.config.getString("redis.masterId")
    val redisConnectionsFactory = new RedisConnectionsFactory(redisClient, redisURI(alarmURI, masterId))
    val redisKeySpaceApiFactory = new RedisKeySpaceApiFactory(redisConnectionsFactory)

    val metadataApi = await(redisConnectionsFactory.wrappedAsyncConnection(MetadataCodec))
    val severityApi = await(redisConnectionsFactory.wrappedAsyncConnection(SeverityCodec))
    val statusApi   = await(redisConnectionsFactory.wrappedAsyncConnection(StatusCodec))

    new AlarmServiceImpl(
      metadataApi,
      severityApi,
      statusApi,
      redisKeySpaceApiFactory,
      new ShelveTimeoutActorFactory()
    )
  }

  private def redisURI(uri: URI, masterId: String) = RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
}
