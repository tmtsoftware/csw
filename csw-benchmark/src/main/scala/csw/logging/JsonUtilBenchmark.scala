package csw.logging

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.persist.JsonOps
import csw.logging.commons.LoggingKeys
import org.openjdk.jmh.annotations._

import scala.collection.JavaConverters.asJavaIterableConverter

/**
 * Tests Json utility performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*JsonUtilBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*JsonUtilBenchmark.*
//

// DEOPSCSW-279: Test logging performance
@State(Scope.Benchmark)
class JsonUtilBenchmark {
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

  val expectedLogMsgJson1: Map[String, String] = JsonOps.Json(logMsgString1).asInstanceOf[Map[String, String]]

  var gson: Gson                        = _
  var jacksonObjectMapper: ObjectMapper = _

  @Setup(Level.Trial)
  def setup() = {
    gson = new Gson()
    jacksonObjectMapper = new ObjectMapper()
  }

  // Benchmark for extracting value of key using persist-json's jgetString method
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchJgetString(): String = {
    JsonOps.jgetString(expectedLogMsgJson1, LoggingKeys.TIMESTAMP)
  }

  // Benchmark for extracting value of key using scala map
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchMapGet(): String = {
    expectedLogMsgJson1(LoggingKeys.TIMESTAMP)
  }

  // Benchmark for json string formation using persist-json library
  // This library provides some additional features than Gson and jackson
  // Ex. Safe check, Sorting by json keys
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchPersistCompact(): String = {
    JsonOps.Compact(expectedLogMsgJson1, safe = true, sort = false)
  }

  // Benchmark for json string formation using gson library
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchGsonJsonConverter(): String = {
    gson.toJson(expectedLogMsgJson1)
  }

  // Benchmark for json string formation using jackson library
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def benchJacksonJsonConverter(): String = {
    jacksonObjectMapper.writeValueAsString(expectedLogMsgJson1.asJava)
  }

}
