package csw.services.event.internal.kafka

import acyclic.skipped
import csw.services.event.javadsl.IEventSubscriber

class JKafkaSubscriber(kafkaSubscriber: KafkaSubscriber) extends IEventSubscriber(kafkaSubscriber)
