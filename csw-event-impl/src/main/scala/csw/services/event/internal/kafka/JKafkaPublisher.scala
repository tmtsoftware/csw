package csw.services.event.internal.kafka

import csw.services.event.internal.pubsub.JBaseEventPublisher

class JKafkaPublisher(kafkaPublisher: KafkaPublisher) extends JBaseEventPublisher(kafkaPublisher)
