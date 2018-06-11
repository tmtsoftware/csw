package csw.services.event.internal.kafka

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.event.internal.commons.EventServiceFactory
import csw.services.event.scaladsl.EventService

import scala.concurrent.ExecutionContext

object KafkaEventServiceFactory extends EventServiceFactory {
  protected override def eventServiceImpl(host: String, port: Int)(
      implicit actorSystem: ActorSystem,
      ec: ExecutionContext,
      mat: Materializer
  ): EventService = new KafkaEventService(host, port)
}
