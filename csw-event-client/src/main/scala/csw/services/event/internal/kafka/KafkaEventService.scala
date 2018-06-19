package csw.services.event.internal.kafka

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Materializer
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.EventService

import scala.concurrent.{ExecutionContext, Future}

class KafkaEventService(eventServiceResolver: EventServiceResolver)(
    implicit actorSystem: ActorSystem,
    val executionContext: ExecutionContext,
    mat: Materializer
) extends EventService {

  override val defaultPublisher: Future[KafkaPublisher]   = publisher()
  override val defaultSubscriber: Future[KafkaSubscriber] = subscriber()

  override def makeNewPublisher(): Future[KafkaPublisher] = publisher()

  private lazy val producerSettings: Future[ProducerSettings[String, Array[Byte]]] = eventServiceResolver.uri.map { uri ⇒
    ProducerSettings(actorSystem, None, None).withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
  }

  private lazy val consumerSettings: Future[ConsumerSettings[String, Array[Byte]]] = eventServiceResolver.uri.map { uri ⇒
    ConsumerSettings(actorSystem, None, None)
      .withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
      .withGroupId(UUID.randomUUID().toString)
  }

  private[csw] def publisher(): Future[KafkaPublisher]   = producerSettings.map(new KafkaPublisher(_))
  private[csw] def subscriber(): Future[KafkaSubscriber] = consumerSettings.map(new KafkaSubscriber(_))
}
