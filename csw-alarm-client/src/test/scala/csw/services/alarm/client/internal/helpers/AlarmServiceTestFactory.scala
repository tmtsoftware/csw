package csw.services.alarm.client.internal.helpers

import akka.actor.ActorSystem
import csw.services.alarm.api.internal.{MetadataKey, SeverityKey, StatusKey}
import csw.services.alarm.api.models.{AlarmMetadata, AlarmSeverity, AlarmStatus}
import csw.services.alarm.client.internal.AlarmCodec.{MetadataCodec, SeverityCodec, StatusCodec}
import csw.services.alarm.client.internal.AlarmServiceImpl
import csw.services.alarm.client.internal.redis.scala_wrapper.{RedisAsyncScalaApi, RedisKeySpaceApi, RedisReactiveScalaApi}
import csw.services.alarm.client.internal.shelve.ShelveTimeoutActorFactory
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.Utf8StringCodec
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

class AlarmServiceTestFactory(redisURI: RedisURI, redisClient: RedisClient)(implicit system: ActorSystem, ec: ExecutionContext) {

  private val metadataAsyncCommands: RedisAsyncCommands[MetadataKey, AlarmMetadata] =
    Await.result(redisClient.connectAsync(MetadataCodec, redisURI).toScala.map(_.async()), 5.seconds)
  private val statusAsyncCommands: RedisAsyncCommands[StatusKey, AlarmStatus] =
    Await.result(redisClient.connectAsync(StatusCodec, redisURI).toScala.map(_.async()), 5.seconds)
  private val severityAsyncCommands: RedisAsyncCommands[SeverityKey, AlarmSeverity] =
    Await.result(redisClient.connectAsync(SeverityCodec, redisURI).toScala.map(_.async()), 5.seconds)
  private val reactiveCommands: RedisPubSubReactiveCommands[String, String] =
    Await.result(redisClient.connectPubSubAsync(new Utf8StringCodec(), redisURI).toScala.map(_.reactive()), 5.seconds)

  val statusApi    = new RedisAsyncScalaApi(statusAsyncCommands)
  val metatdataApi = new RedisAsyncScalaApi(metadataAsyncCommands)
  val severityApi  = new RedisAsyncScalaApi(severityAsyncCommands)

  def make(): AlarmServiceImpl = new AlarmServiceImpl(
    metatdataApi,
    severityApi,
    statusApi,
    () ⇒
      new RedisKeySpaceApi[StatusKey, AlarmStatus](
        () => new RedisReactiveScalaApi[String, String](reactiveCommands),
        statusApi
    ),
    new ShelveTimeoutActorFactory()
  )
}
