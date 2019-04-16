package csw.benchmark.logging

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import csw.benchmark.logging.mock.LogActorMock
import csw.logging.client.appenders.FileAppender
import csw.logging.client.internal.LoggingSystem
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

// DEOPSCSW-279: Test logging performance
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

  // This benchmark is for file appender. The result of this benchmark will be the number of messages
  // actually written to the file
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def fileAppenderThroughput(): Unit = {
    LogActorMock.receiveLog(fileAppender)
  }
}
