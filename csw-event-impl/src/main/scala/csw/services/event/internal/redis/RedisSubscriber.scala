package csw.services.event.internal.redis

import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, Materializer}
import akka.{Done, NotUsed}
import csw.messages.events._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import io.lettuce.core.{RedisClient, RedisURI}
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisSubscriber(redisURI: RedisURI, redisClient: RedisClient)(
    implicit ec: ExecutionContext,
    protected val mat: Materializer
) extends EventSubscriber {

  private lazy val asyncConnectionF: Future[RedisAsyncCommands[EventKey, Event]] =
    redisClient.connectAsync(EventServiceCodec, redisURI).toScala.map(_.async())

  private def reactiveConnectionF(): Future[RedisPubSubReactiveCommands[EventKey, Event]] =
    redisClient.connectPubSubAsync(EventServiceCodec, redisURI).toScala.map(_.reactive())

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val connectionF       = reactiveConnectionF()
    val latestEventStream = Source(eventKeys).mapAsync(eventKeys.size)(get)
    val eventStream       = Source.fromFuture(connectionF).flatMapConcat(connection ⇒ subscribe(eventKeys, connection))

    latestEventStream
      .concat(eventStream)
      .viaMat(KillSwitches.single)(Keep.right)
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, terminationSignal) ⇒
          new EventSubscription {
            override def unsubscribe(): Future[Done] = async {
              val commands = await(connectionF)
              await(commands.unsubscribe(eventKeys.toSeq: _*).toFuture.toScala)
              killSwitch.shutdown()
              await(terminationSignal)
            }
          }
      }
  }

  override def get(eventKeys: Set[EventKey]): Future[Set[Event]] = {
    Future.sequence(eventKeys.map(get))
  }

  override def get(eventKey: EventKey): Future[Event] = async {
    val connection = await(asyncConnectionF)
    val event      = await(connection.get(eventKey).toScala)
    if (event == null) Event.invalidEvent else event
  }

  private def subscribe(
      eventKeys: Set[EventKey],
      connection: RedisPubSubReactiveCommands[EventKey, Event]
  ): Source[Event, NotUsed] =
    Source
      .fromFuture(connection.subscribe(eventKeys.toSeq: _*).toFuture.toScala.map(_ ⇒ ()))
      .flatMapConcat(_ ⇒ Source.fromPublisher(connection.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage))
}
