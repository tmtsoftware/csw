package csw.event.client.internal.kafka

import java.util.UUID

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Attributes
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.serviceresolver.EventServiceResolver

import scala.concurrent.{ExecutionContext, Future}

/**
 * Implementation of [[csw.event.api.scaladsl.EventService]] which provides handle to [[csw.event.api.scaladsl.EventPublisher]]
 * and [[csw.event.api.scaladsl.EventSubscriber]] backed by Kafka
 *
 * @param eventServiceResolver to get the connection information of event service
 * @param actorSystem actor system to be used by Producer and Consumer API of akka-stream-kafka
 * @param attributes the attributes used for materializing underlying streams
 */
// $COVERAGE-OFF$
private[event] class KafkaEventService(eventServiceResolver: EventServiceResolver)(
    implicit actorSystem: ActorSystem[_],
    attributes: Attributes
) extends EventService {

  implicit val executionContext: ExecutionContext = actorSystem.executionContext

  override def makeNewPublisher(): KafkaPublisher   = new KafkaPublisher(producerSettings)
  override def makeNewSubscriber(): KafkaSubscriber = new KafkaSubscriber(consumerSettings)

  // resolve event service every time before creating a new publisher
  private def producerSettings: Future[ProducerSettings[String, Array[Byte]]] = eventServiceResolver.uri().map { uri =>
    ProducerSettings(actorSystem.toClassic, None, None).withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
  }

  // resolve event service every time before creating a new subscriber
  private def consumerSettings: Future[ConsumerSettings[String, Array[Byte]]] = eventServiceResolver.uri().map { uri =>
    ConsumerSettings(actorSystem.toClassic, None, None)
      .withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
      .withGroupId(UUID.randomUUID().toString)
  }

}
// $COVERAGE-ON$
