package csw.services.alarm.client

import java.util.concurrent.CompletableFuture

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.scaladsl.{AlarmAdminService, AlarmService}
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

import scala.async.Async.async
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.{ExecutionContext, Future}

class AlarmServiceFactory(redisClient: RedisClient = RedisClient.create()) {

  def this() = this(RedisClient.create())

  def adminApi(locationService: LocationService)(implicit system: ActorSystem): Future[AlarmAdminService] = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceLocationResolver(locationService))
  }

  def adminApi(host: String, port: Int)(implicit system: ActorSystem): Future[AlarmAdminService] = {
    implicit val ec: ExecutionContext = system.dispatcher
    alarmService(new AlarmServiceHostPortResolver(host, port))
  }

  def clientApi(locationService: LocationService)(implicit system: ActorSystem): Future[AlarmService] = adminApi(locationService)

  def clientApi(host: String, port: Int)(implicit system: ActorSystem): Future[AlarmService] = adminApi(host, port)

  def jClientApi(locationService: ILocationService, system: ActorSystem): CompletableFuture[IAlarmService] = {
    implicit val ec: ExecutionContext        = system.dispatcher
    val alarmServiceF: Future[IAlarmService] = adminApi(locationService.asScala)(system).map(new JAlarmServiceImpl(_))
    alarmServiceF.toJava.toCompletableFuture
  }

  def jClientApi(host: String, port: Int, system: ActorSystem): CompletableFuture[IAlarmService] = {
    implicit val ec: ExecutionContext        = system.dispatcher
    val alarmServiceF: Future[IAlarmService] = adminApi(host, port)(system).map(new JAlarmServiceImpl(_))
    alarmServiceF.toJava.toCompletableFuture
  }

  /************ INTERNAL ************/
  private def alarmService(alarmServiceResolver: AlarmServiceResolver)(implicit system: ActorSystem, ec: ExecutionContext) =
    async {
      val settings                = new Settings(ConfigFactory.load())
      val redisConnectionsFactory = new RedisConnectionsFactory(redisClient, alarmServiceResolver, settings.masterId)

      new AlarmServiceImpl(
        redisConnectionsFactory,
        new ShelveTimeoutActorFactory(),
        settings
      )
    }
}
