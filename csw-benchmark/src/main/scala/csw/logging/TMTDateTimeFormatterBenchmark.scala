package csw.logging

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import com.persist.JsonOps
import csw.logging.commons.{LoggingKeys, TMTDateTimeFormatter}
import org.openjdk.jmh.annotations._

/**
 * Tests TMTDateTimeFormatterBenchmark performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*TMTDateTimeFormatterBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*TMTDateTimeFormatterBenchmark.*
//

// DEOPSCSW-279: Test logging performance
@State(Scope.Benchmark)
class TMTDateTimeFormatterBenchmark {
  var logMsgString1: String = s"""{
                                 |  "${LoggingKeys.CATEGORY}": "alternative",
                                 |  "${LoggingKeys.COMPONENT_NAME}": "FileAppenderTest",
                                 |  "${LoggingKeys.HOST}": "localhost",
                                 |  "${LoggingKeys.NAME}": "test-service",
                                 |  "${LoggingKeys.SEVERITY}": "ERROR",
                                 |  "${LoggingKeys.TIMESTAMP}": "2017-06-19T16:10:19.397Z",
                                 |  "${LoggingKeys.CLASS}": "csw.logging.appenders.FileAppenderTest",
                                 |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
                                 |  "${LoggingKeys.LINE}": 25,
                                 |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
                                 |}
    """.stripMargin

  val expectedLogMsgJson1 = JsonOps.Json(logMsgString1).asInstanceOf[Map[String, String]]

  val timestamp = System.currentTimeMillis()
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchTMTFormat(): String = {
    TMTDateTimeFormatter.format(timestamp)
  }

  val timestampStr = expectedLogMsgJson1(LoggingKeys.TIMESTAMP)
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchTMTParse(): ZonedDateTime = {
    TMTDateTimeFormatter.parse(timestampStr)
  }

}
