package csw.services.event.internal.kafka

import acyclic.skipped
import csw.services.event.javadsl.IEventPublisher

class JKafkaPublisher(kafkaPublisher: KafkaPublisher) extends IEventPublisher(kafkaPublisher)
