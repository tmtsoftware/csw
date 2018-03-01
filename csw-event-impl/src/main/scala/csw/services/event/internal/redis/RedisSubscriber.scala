package csw.services.event.internal.redis

import akka.Done
import akka.stream.scaladsl.{Concat, Keep, Source}
import akka.stream.{KillSwitches, Materializer}
import csw.messages.ccs.events._
import csw.services.event.scaladsl.{EventSubscriber, EventSubscription}
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.async.Async._
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}

class RedisSubscriber(redisGateway: RedisGateway)(implicit ec: ExecutionContext, protected val mat: Materializer)
    extends EventSubscriber {
  private val asyncConnectionF = redisGateway.asyncConnectionF()

  override def subscribe(eventKeys: Set[EventKey]): Source[Event, EventSubscription] = {
    val co̦nnectionF = redisGateway.reactiveConnectionF()

    val sourceF = async {
      val connection = await(co̦nnectionF)
      connection.subscribe(eventKeys.toSeq: _*).subscribe()
      Source.fromPublisher(connection.observeChannels(OverflowStrategy.LATEST)).map(_.getMessage)
    }

    val eventStream       = Source.fromFutureSource(sourceF)
    val latestEventStream = Source.fromFutureSource(get(eventKeys).map(events ⇒ Source(events.filterNot(_ == null))))

    Source
      .combine(latestEventStream, eventStream)(Concat[Event])
      .viaMat(KillSwitches.single)(Keep.right)
      .watchTermination()(Keep.both)
      .mapMaterializedValue {
        case (killSwitch, doneF) ⇒
          new EventSubscription {
            override def unsubscribe(): Future[Done] = async {
              val commands = await(co̦nnectionF)
              await(commands.unsubscribe(eventKeys.toSeq: _*).toFuture.toScala)
              killSwitch.shutdown()
              await(doneF)
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
}
