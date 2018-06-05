package csw.params

import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.serialization.{Serialization, SerializationExtension}
import csw.messages.commands.{CommandName, Observe}
import csw.messages.params.generics.KeyType.ByteArrayKey
import csw.messages.params.generics.{Key, Parameter}
import csw.messages.params.models.Units.pascal
import csw.messages.params.models.{ArrayData, ObsId, Prefix}
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

/**
 * Tests ImageDeSerializationBenchmark performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*ImageDeSerializationBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*ImageDeSerializationBenchmark.*
//

// DEOPSCSW-187: Efficient serialization to/from binary
@State(Scope.Benchmark)
class ImageDeSerializationBenchmark {
  private final var system: ActorSystem          = _
  private final var serialization: Serialization = _
  private final var prefixStr: String            = _
  private final var obsId: ObsId                 = _

  private final var img_32k_tuple: (Array[Byte], Observe)  = _
  private final var img_128k_tuple: (Array[Byte], Observe) = _
  private final var img_512k_tuple: (Array[Byte], Observe) = _

  @Setup(Level.Trial)
  def setup(): Unit = {
    system = ActorSystem("example")
    serialization = SerializationExtension(system)
    prefixStr = "wfos.prog.cloudcover"
    obsId = ObsId("Obs001")

    img_32k_tuple = serializeImage("/images/32k_image.bin")
    img_128k_tuple = serializeImage("/images/128k_image.bin")
    img_512k_tuple = serializeImage("/images/512k_image.bin")
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    Await.result(system.terminate(), 5.seconds)
  }

  def serializeImage(file: String): (Array[Byte], Observe) = {
    val path                           = Paths.get(getClass.getResource(file).getPath)
    val binaryData                     = Files.readAllBytes(path)
    val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make("imageKey")

    val binaryImgData: ArrayData[Byte]    = ArrayData.fromArray(binaryData)
    val param: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal

    val observe           = Observe(Prefix("originationPrefix"), CommandName(prefixStr), Some(obsId)).add(param)
    val observeSerializer = serialization.findSerializerFor(observe)

    (observeSerializer.toBinary(observe), observe)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def _032kImageDeSerializationBench(): Observe = {
    val observeSerializer = serialization.findSerializerFor(img_32k_tuple._2)
    observeSerializer.fromBinary(img_32k_tuple._1).asInstanceOf[Observe]
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def _128kImageDeSerializationBench(): Observe = {
    val observeSerializer = serialization.findSerializerFor(img_128k_tuple._2)
    observeSerializer.fromBinary(img_128k_tuple._1).asInstanceOf[Observe]
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def _512kImageDeSerializationBench(): Observe = {
    val observeSerializer = serialization.findSerializerFor(img_512k_tuple._2)
    observeSerializer.fromBinary(img_512k_tuple._1).asInstanceOf[Observe]
  }
}
