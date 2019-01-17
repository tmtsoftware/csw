package csw.benchmark.logging

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import csw.logging.client.commons.{LoggingKeys, TMTDateTimeFormatter}
import csw.logging.client.internal.JsonExtensions.RichJsObject
import org.openjdk.jmh.annotations._
import play.api.libs.json.{JsObject, Json}

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
                                 |  "${LoggingKeys.CLASS}": "csw.logging.client.appenders.FileAppenderTest",
                                 |  "${LoggingKeys.FILE}": "FileAppenderTest.scala",
                                 |  "${LoggingKeys.LINE}": 25,
                                 |  "${LoggingKeys.MESSAGE}": "This is at ERROR level"
                                 |}
    """.stripMargin

  val expectedLogMsgJson1 = Json.parse(logMsgString1).as[JsObject]

  val timestamp = System.currentTimeMillis()
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchTMTFormat(): String = {
    TMTDateTimeFormatter.format(timestamp)
  }

  val timestampStr = expectedLogMsgJson1.getString(LoggingKeys.TIMESTAMP)

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchTMTParse(): ZonedDateTime = {
    TMTDateTimeFormatter.parse(timestampStr)
  }

}
