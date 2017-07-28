package csw.services.logging.perf.jmh

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import csw.services.logging.appenders.FileAppender
import csw.services.logging.internal.LoggingLevels.INFO
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.{Logger, LoggerImpl}
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

/**
 * Tests CSW E2E logging performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*E2ELoggingBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*E2ELoggingBenchmark.*
//

// DEOPSCSW-279: Test logging performance
@State(Scope.Benchmark)
class E2ELoggingBenchmark {
  var actorSystem: ActorSystem   = _
  var log: Logger                = _
  var fileAppender: FileAppender = _
  var person: Person             = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    actorSystem = ActorSystem("logging")
    val loggingSystem = new LoggingSystem("E2E", "SNAPSHOT-1.0", InetAddress.getLocalHost.getHostName, actorSystem)
    loggingSystem.setAppenders(List(FileAppender))
    loggingSystem.setDefaultLogLevel(INFO)
    log = new LoggerImpl(None, None)
    person = Person.createDummy
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def e2eLoggingThroughput(): Unit = {
    log.info(s"My name is $person, logging with ByNameAPI@INFO Level")
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def e2eLoggingThroughputWhenLogLevelIsNotEnabled(): Unit = {
    log.trace(s"My name is $person, logging with ByNameAPI@TRACE Level")
  }
}

object Person {
  private[jmh] def createDummy = Person("James", "Bond", "Downtown", "Omaha")
}

case class Person(var firstName: String, var lastName: String, var address: String, var city: String)
