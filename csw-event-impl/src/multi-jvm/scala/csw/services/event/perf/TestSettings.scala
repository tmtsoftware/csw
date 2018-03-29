package csw.services.event.perf

import akka.actor.ActorSystem

final case class TestSettings(testName: String, totalMessages: Long, burstSize: Int, payloadSize: Int, senderReceiverPairs: Int) {
  // data based on measurement
  def totalSize(system: ActorSystem): Int = payloadSize + 110
}
