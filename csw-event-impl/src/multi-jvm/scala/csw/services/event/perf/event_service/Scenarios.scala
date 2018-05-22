package csw.services.event.perf.event_service

import csw.services.event.perf.wiring.TestConfigs
case class Scenario(name: String, testSettings: List[TestSettings])

class Scenarios(testConfigs: TestConfigs) {
  import testConfigs._

  def adjustedTotalMessages(n: Long): Long = (n * totalMessagesFactor).toLong

  val warmUp = TestSettings(
    testName = "warm-up",
    totalTestMsgs = adjustedTotalMessages(10000),
    payloadSize = 100,
    publisherSubscriberPairs = 1,
    singlePublisher = false
  )

  val payloadOneToOne: Scenario = Scenario(
    "payload-1-to-1",
    List(
      TestSettings(
        testName = "1-to-1-size-100",
        totalTestMsgs = adjustedTotalMessages(2000),
        payloadSize = 100,
        publisherSubscriberPairs = 1,
        singlePublisher = false
      ),
//      TestSettings(
//        testName = "1-to-1-size-1KB",
//        totalTestMsgs = adjustedTotalMessages(2000),
//        payloadSize = 1024,
//        publisherSubscriberPairs = 1,
//        singlePublisher = false
//      )
//    TestSettings(
//      testName = "1-to-1-size-10KB",
//      totalTestMsgs = adjustedTotalMessages(10000),
//      payloadSize = 10 * 1024,
//      publisherSubscriberPairs = 1,
//      singlePublisher = false
//    ),
//    TestSettings(
//      testName = "1-to-1-size-100KB",
//      totalTestMsgs = adjustedTotalMessages(10000),
//      payloadSize = 100 * 1024,
//      publisherSubscriberPairs = 1,
//      singlePublisher = false
//    ),
//    TestSettings(
//      testName = "1-to-1-size-300KB",
//      totalTestMsgs = adjustedTotalMessages(10000),
//      payloadSize = 300 * 1024,
//      publisherSubscriberPairs = 1,
//      singlePublisher = false
//    ),
//    TestSettings(
//      testName = "1-to-1-size-500KB",
//      totalTestMsgs = adjustedTotalMessages(10000),
//      payloadSize = 500 * 1024,
//      publisherSubscriberPairs = 1,
//      singlePublisher = false
//    ),
//    TestSettings(
//      testName = "1-to-1-size-600KB",
//      totalTestMsgs = adjustedTotalMessages(10000),
//      payloadSize = 600 * 1024,
//      publisherSubscriberPairs = 1,
//      singlePublisher = false
//    )
    )
  )

  val payloadOneToMany: Scenario = Scenario(
    "payload-1-to-Many",
    List(
      TestSettings(
        testName = "1-to-5-size-100",
        totalTestMsgs = adjustedTotalMessages(2000),
        payloadSize = 100,
        publisherSubscriberPairs = 5,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-5-size-1KB",
        totalTestMsgs = adjustedTotalMessages(2000),
        payloadSize = 1024,
        publisherSubscriberPairs = 5,
        singlePublisher = true
      )
    )
  )

  val oneToMany: Scenario = Scenario(
    "one-to-many",
    List(
      TestSettings(
        testName = "1-to-1",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 1,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-5",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 5,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-10",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 10,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-50",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 50,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-100",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 100,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-200",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 200,
        singlePublisher = true
      )
    )
  )

  val manyToMany: Scenario = Scenario(
    "many-to-many",
    List(
      TestSettings(
        testName = "one-to-one",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 1,
        singlePublisher = false
      ),
      TestSettings(
        testName = "5-to-5",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 5,
        singlePublisher = false
      ),
      TestSettings(
        testName = "10-to-10",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 10,
        singlePublisher = false
      ),
      TestSettings(
        testName = "25-to-25",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 25,
        singlePublisher = false
      ),
      TestSettings(
        testName = "50-to-50",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 50,
        singlePublisher = false
      ),
      TestSettings(
        testName = "60-to-60",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 100,
        publisherSubscriberPairs = 60,
        singlePublisher = false
      ),
      TestSettings(
        testName = "70-to-70",
        totalTestMsgs = adjustedTotalMessages(10000),
        payloadSize = 70,
        publisherSubscriberPairs = 70,
        singlePublisher = false
      )
    )
  )

  val normal: Scenario = Scenario(
    "all",
    List(
      TestSettings(
        testName = "1-to-1",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 100,
        publisherSubscriberPairs = 1,
        singlePublisher = false
      ),
      TestSettings(
        testName = "1-to-1-size-1k",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 1000,
        publisherSubscriberPairs = 1,
        singlePublisher = false
      ),
      TestSettings(
        testName = "1-to-1-size-10k",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 1000,
        publisherSubscriberPairs = 1,
        singlePublisher = false
      ),
      TestSettings(
        testName = "5-to-5",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 100,
        publisherSubscriberPairs = 5,
        singlePublisher = false
      ),
      TestSettings(
        testName = "10-to-10",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 100,
        publisherSubscriberPairs = 10,
        singlePublisher = false
      ),
      TestSettings(
        testName = "1-to-5",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 100,
        publisherSubscriberPairs = 5,
        singlePublisher = true
      ),
      TestSettings(
        testName = "1-to-10",
        totalTestMsgs = adjustedTotalMessages(5000),
        payloadSize = 100,
        publisherSubscriberPairs = 10,
        singlePublisher = true
      )
    )
  )

  val all: List[Scenario] = List(payloadOneToOne, oneToMany, manyToMany)
}
