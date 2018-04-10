package csw.services.event.perf

final case class TestSettings(
    testName: String,
    totalMessages: Long,
    burstSize: Int,
    payloadSize: Int,
    publisherSubscriberPairs: Int,
    batching: Boolean,
    singlePublisher: Boolean
) {
  // data based on measurement
  def totalSize: Int = payloadSize + 97
}
