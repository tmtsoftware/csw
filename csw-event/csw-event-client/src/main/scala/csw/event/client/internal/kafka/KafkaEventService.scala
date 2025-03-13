/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.client.internal.kafka

import java.util.UUID

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter.TypedActorSystemOps
import org.apache.pekko.kafka.{ConsumerSettings, ProducerSettings}
import csw.event.api.scaladsl.EventService
import csw.event.client.internal.commons.serviceresolver.EventServiceResolver

import scala.concurrent.Future

/**
 * Implementation of [[csw.event.api.scaladsl.EventService]] which provides handle to [[csw.event.api.scaladsl.EventPublisher]]
 * and [[csw.event.api.scaladsl.EventSubscriber]] backed by Kafka
 *
 * @param eventServiceResolver to get the connection information of event service
 * @param actorSystem actor system to be used by Producer and Consumer API of pekko-connectors-kafka
 */
// $COVERAGE-OFF$
private[event] class KafkaEventService(eventServiceResolver: EventServiceResolver)(implicit
    actorSystem: ActorSystem[?]
) extends EventService {

  import actorSystem.executionContext
  override def makeNewPublisher(): KafkaPublisher   = new KafkaPublisher(producerSettings)
  override def makeNewSubscriber(): KafkaSubscriber = new KafkaSubscriber(consumerSettings)

  // resolve event service every time before creating a new publisher
  private def producerSettings: Future[ProducerSettings[String, Array[Byte]]] =
    eventServiceResolver.uri().map { uri =>
      ProducerSettings(actorSystem.toClassic, None, None).withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
    }

  // resolve event service every time before creating a new subscriber
  private def consumerSettings: Future[ConsumerSettings[String, Array[Byte]]] =
    eventServiceResolver.uri().map { uri =>
      ConsumerSettings(actorSystem.toClassic, None, None)
        .withBootstrapServers(s"${uri.getHost}:${uri.getPort}")
        .withGroupId(UUID.randomUUID().toString)
    }

}
// $COVERAGE-ON$
