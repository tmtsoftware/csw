package csw.services.logging.perf.jmh

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import csw.services.logging.appenders.FileAppender
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.perf.jmh.mock.LogActorMock
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

/**
 * Tests CSW File Appender performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*FileAppenderBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*FileAppenderBenchmark.*
//
@State(Scope.Benchmark)
class FileAppenderBenchmark {
  var actorSystem: ActorSystem   = _
  var fileAppender: FileAppender = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    actorSystem = ActorSystem("logging")
    new LoggingSystem("FileAppender", "SNAPSHOT-1.0", InetAddress.getLocalHost.getHostName, actorSystem)
    fileAppender = new FileAppender(actorSystem, LogActorMock.standardHeaders)
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def fileAppenderThroughput(): Unit = {
    LogActorMock.receiveLog(fileAppender)
  }
}
