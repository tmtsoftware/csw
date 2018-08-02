package csw.services.alarm.client

import java.net.URI

import akka.actor.ActorSystem
import csw.services.alarm.api.internal.StatusKey
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.models.AlarmStatus
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.{AlarmServiceImpl, JAlarmServiceImpl}
import csw.services.alarm.client.internal.commons.{AlarmServiceLocationResolver, ConnectionsFactory}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.codec.Utf8StringCodec
import io.lettuce.core.{RedisClient, RedisURI}
import romaine.RedisKeySpaceApi

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
    val masterId           = actorSystem.settings.config.getString("redis.masterId")
    val connectionsFactory = new ConnectionsFactory(redisClient, redisURI(alarmURI, masterId))

    val metadataApi = await(connectionsFactory.wrappedAsyncConnection(MetadataCodec))
    val severityApi = await(connectionsFactory.wrappedAsyncConnection(SeverityCodec))
    val statusApi   = await(connectionsFactory.wrappedAsyncConnection(StatusCodec))

    // TODO: simplify
    val statusStreamApiFactory = await(
      connectionsFactory
        .wrappedReactiveConnection(new Utf8StringCodec())
        .map { conn ⇒ () ⇒
          new RedisKeySpaceApi[StatusKey, AlarmStatus](() ⇒ conn, statusApi)
        }
    )

    new AlarmServiceImpl(
      metadataApi,
      severityApi,
      statusApi,
      statusStreamApiFactory,
      new ShelveTimeoutActorFactory()
    )
  }

  private[alarm] def redisURI(uri: URI, masterId: String) = RedisURI.Builder.sentinel(uri.getHost, uri.getPort, masterId).build()
}
