package csw.services.event.perf

import java.util.concurrent.TimeUnit.SECONDS

class TestRateReporter(name: String)
    extends RateReporter(
      SECONDS.toNanos(1),
      new RateReporter.Reporter {
        override def onReport(messagesPerSec: Double, bytesPerSec: Double, totalMessages: Long, totalBytes: Long): Unit = {
          println(
            name +
            f": $messagesPerSec%,.0f msgs/sec, $bytesPerSec%,.0f bytes/sec, " +
            f"totals $totalMessages%,d messages ${totalBytes / (1024 * 1024)}%,d MB"
          )
        }
      }
    ) {}
