package csw.services.alarm.client.internal

import csw.services.alarm.api.models.{AlarmKey, AlarmMetadata, AlarmSeverity}
import csw.services.alarm.api.scaladsl.AlarmAdminService
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class AlarmServiceImpl(redisURI: RedisURI, redisClient: RedisClient)(implicit ec: ExecutionContext) extends AlarmAdminService {

  private lazy val asyncCommandsF: Future[RedisAsyncCommands[AlarmKey, AlarmMetadata]] = Future.unit
    .flatMap(_ ⇒ redisClient.connectAsync(AlarmMetadataCodec, redisURI).toScala)
    .map(_.async())

  override def setSeverity(key: AlarmKey, severity: AlarmSeverity): Future[Unit] = async {
    val commands = await(asyncCommandsF)
    val metadata = await(commands.get(key).toScala)
  }

  override def getSeverity(key: AlarmKey): Future[AlarmSeverity] = ???
}
