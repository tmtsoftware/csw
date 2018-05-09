package csw.services.event.internal.redis

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import csw.messages.events.{Event, EventKey}
import csw.services.event.commons.EventServiceLogger
import csw.services.event.exceptions.PublishFailed
import csw.services.event.internal.pubsub.{AbstractEventPublisher, JAbstractEventPublisher}
import csw.services.event.javadsl.IEventPublisher
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.{RedisClient, RedisURI}

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class RedisPublisherWithSetActor(
    redisURI: RedisURI,
    redisClient: RedisClient,
    system: ActorSystem
)(implicit ec: ExecutionContext, mat: Materializer)
    extends AbstractEventPublisher {

  private val logger = EventServiceLogger.getLogger

  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  private lazy val setActor = asyncConnectionF.map(connection ⇒ system.spawn(SetBehavior.setBehavior(connection), "redis-set"))

  override def publish[Mat](source: Source[Event, Mat], onError: (Event, PublishFailed) ⇒ Unit): Mat =
    publishWithRecovery(source, Some(onError))

  override def publish[Mat](source: Source[Event, Mat]): Mat = publishWithRecovery(source, None)

  override def publish(event: Event): Future[Done] =
    async {
      val commands = await(asyncConnectionF)
      // allow publish and set to run in parallel
      val publishF = commands.publish(event.eventKey, event).toScala
      await(publishF)
      await(setActor) ! RedisMessage.Set(event)
      Done
    } recover {
      case NonFatal(ex) ⇒ throw PublishFailed(event, ex.getMessage)
    }

  override def shutdown(): Future[Done] = asyncConnectionF.flatMap(_.quit().toScala).map(_ ⇒ Done)

  private def publishWithRecovery[Mat](source: Source[Event, Mat], maybeOnError: Option[(Event, PublishFailed) ⇒ Unit]): Mat =
    source
      .mapAsync(1) {
        maybeOnError match {
          case Some(onError) ⇒ publish(_).recover { case ex @ PublishFailed(event, _) ⇒ onError(event, ex) }
          case None          ⇒ publish
        }
      }
      .mapError {
        case NonFatal(ex) ⇒
          logger.error(ex.getMessage, ex = ex)
          ex
      }
      .to(Sink.ignore)
      .run()

  override def asJava: IEventPublisher = new JAbstractEventPublisher(this) {}
}
