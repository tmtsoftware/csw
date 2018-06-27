package csw.services.event.internal.kafka

import java.util.UUID

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.stream.Materializer
import csw.services.event.internal.commons.serviceresolver.EventServiceResolver
import csw.services.event.scaladsl.EventService

import scala.concurrent.{ExecutionContext, Future}

class KafkaEventService(eventServiceResolver: EventServiceResolver)(implicit actorSystem: ActorSystem, mat: Materializer)
    extends EventService {

  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  override def makeNewPublisher(): Future[KafkaPublisher] = producerSettings.map(new KafkaPublisher(_))

  override def makeNewSubscriber(): Future[KafkaSubscriber] = consumerSettings.map(new KafkaSubscriber(_))

  // resolve event service every time before creating a new publisher
  private def producerSettings: Future[ProducerSettings[String, Array[Byte]]] = eventServiceResolver.uri().map { uri ⇒
    ProducerSettings(actorSystem, None, None).withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
  }

  // resolve event service every time before creating a new subscriber
  private def consumerSettings: Future[ConsumerSettings[String, Array[Byte]]] = eventServiceResolver.uri().map { uri ⇒
    ConsumerSettings(actorSystem, None, None)
      .withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
      .withGroupId(UUID.randomUUID().toString)
  }

}
