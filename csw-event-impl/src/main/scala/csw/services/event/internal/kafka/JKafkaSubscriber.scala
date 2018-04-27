package csw.services.event.internal.kafka

import csw.services.event.internal.pubsub.JBaseEventSubscriber

class JKafkaSubscriber(kafkaSubscriber: KafkaSubscriber) extends JBaseEventSubscriber(kafkaSubscriber)
