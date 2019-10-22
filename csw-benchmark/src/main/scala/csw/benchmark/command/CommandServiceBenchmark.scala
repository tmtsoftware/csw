package csw.benchmark.command

import java.util.concurrent.TimeUnit

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.Scheduler
import akka.util
import com.typesafe.config.ConfigFactory
import csw.benchmark.command.BenchmarkHelpers.spawnStandaloneComponent
import csw.command.api.scaladsl.CommandService
import csw.location.server.internal.ServerWiring
import csw.params.commands
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.models.Prefix
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*CommandServiceBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*CommandServiceBenchmark.*
//

// DEOPSCSW-231 :Measure Performance of Command Service
@State(Scope.Benchmark)
@Fork(1)
@Threads(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS, batchSize = 1)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS, batchSize = 1)
class CommandServiceBenchmark {

  implicit var timeout: util.Timeout    = _
  implicit var scheduler: Scheduler     = _
  var setupCommand: commands.Setup      = _
  var componentRef: CommandService      = _
  private var adminWiring: ServerWiring = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    adminWiring = ServerWiring.make(Some(3553))
    Await.result(adminWiring.locationHttpService.start(), 5.seconds)
    componentRef = spawnStandaloneComponent(adminWiring.actorSystem, ConfigFactory.load("standalone.conf"))
    setupCommand = commands.Setup(Prefix("wfos.blue.filter"), CommandName("jmh"), None)
    timeout = util.Timeout(5.seconds)
    scheduler = adminWiring.actorSystem.scheduler
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    Await.result(adminWiring.actorRuntime.shutdown(UnknownReason), 5.seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def commandThroughput(): SubmitResponse = {
    Await.result(componentRef.submitAndWait(setupCommand), 5.seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def commandLatency(): SubmitResponse = {
    Await.result(componentRef.submitAndWait(setupCommand), 5.seconds)
  }
}
