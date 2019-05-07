package csw.benchmark.event
import java.util.concurrent.TimeUnit

import csw.event.client.pb.PbConverter
import csw.params.core.generics.{Key, KeyType, Parameter}
import csw.params.core.models.{Prefix, RaDec, Struct}
import csw.params.events.{EventName, SystemEvent}
import csw_protobuf.events.PbEvent
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
class ProtobufSerializationBenchmark {

  private val byteKey   = KeyType.ByteKey.make("bytes")
  private val intKey    = KeyType.IntKey.make("ints")
  private val doubleKey = KeyType.DoubleKey.make("doubles")
  private val floatKey  = KeyType.FloatKey.make("floats")
  private val stringKey = KeyType.StringKey.make("strings")
  private val radecKey  = KeyType.RaDecKey.make("radecs")

  private val paramSet: Set[Parameter[_]] = Set(
    byteKey.set(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    intKey.set(100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100),
    doubleKey.set(100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342,
      100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342, 100.342),
    floatKey.set(100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f,
      100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f, 100.342f),
    stringKey.set(
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100",
      "100"
    ),
    radecKey.set(
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100),
      RaDec(100, 100)
    )
  )
  private val structKey: Key[Struct] = KeyType.StructKey.make("abc")
  //  private val structKey2: Key[Struct] = KeyType.StructKey.make("abc2")
  val data: Parameter[Struct] = structKey.set((1 to 150).map(_ => Struct(paramSet)): _*)
  val event = SystemEvent(Prefix("a.b"), EventName("eventName1")).copy(
    paramSet = Set(data)
  )

  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def pbThrpt(): SystemEvent = {
    val bytes: Array[Byte] = PbConverter.toPbEvent(event).toByteArray
    PbConverter.fromPbEvent[SystemEvent](PbEvent.parseFrom(bytes))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def pbAvgTime(): SystemEvent = {
    val bytes: Array[Byte] = PbConverter.toPbEvent(event).toByteArray
    PbConverter.fromPbEvent[SystemEvent](PbEvent.parseFrom(bytes))
  }

}

object A extends App {
  val bytes: Array[Byte] = PbConverter.toPbEvent(new ProtobufSerializationBenchmark().event).toByteArray
  println(bytes.length)
}
