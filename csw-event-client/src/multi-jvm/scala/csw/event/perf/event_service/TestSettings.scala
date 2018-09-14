package csw.event.perf.event_service

case class TestSettings(
    testName: String,
    totalTestMsgs: Long,
    payloadSize: Int,
    publisherSubscriberPairs: Int,
    singlePublisher: Boolean
)
