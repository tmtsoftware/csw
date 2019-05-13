package csw.benchmark.event

import java.util.concurrent.TimeUnit

import csw.event.client.pb.PbConverter
import csw.params.events.SystemEvent
import csw_protobuf.events.PbEvent
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
class ProtobufSerializationBenchmark {
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def pbThrpt(): SystemEvent = {
    val bytes: Array[Byte] = PbConverter.toPbEvent(Data.bigEvent).toByteArray
    PbConverter.fromPbEvent[SystemEvent](PbEvent.parseFrom(bytes))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def pbAvgTime(): SystemEvent = {
    val bytes: Array[Byte] = PbConverter.toPbEvent(Data.bigEvent).toByteArray
    PbConverter.fromPbEvent[SystemEvent](PbEvent.parseFrom(bytes))
  }

}

object SimplePbTest extends App {
  val bytes: Array[Byte] = PbConverter.toPbEvent(Data.bigEvent).toByteArray
  println(bytes.length)
}
