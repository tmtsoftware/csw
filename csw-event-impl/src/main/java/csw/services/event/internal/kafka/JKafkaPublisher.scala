package csw.services.event.internal.kafka

import csw.services.event.javadsl.IEventPublisher

class JKafkaPublisher(kafkaPublisher: KafkaPublisher) extends IEventPublisher(kafkaPublisher)
