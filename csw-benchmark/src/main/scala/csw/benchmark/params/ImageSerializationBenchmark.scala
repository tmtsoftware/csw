/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.benchmark.params

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.TimeUnit

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.serialization.{Serialization, SerializationExtension}
import csw.params.commands.{CommandName, Observe}
import csw.params.core.generics.KeyType.ByteArrayKey
import csw.params.core.generics.{Key, Parameter}
import csw.params.core.models.Units.pascal
import csw.params.core.models.{ArrayData, ObsId}
import csw.prefix.models.Prefix
import org.openjdk.jmh.annotations._

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

/**
 * Tests ImageSerializationBenchmark performance.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
//
// single thread:
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 .*ImageSerializationBenchmark.*
//
// multiple threads (for example, 4 threads):
// sbt csw-benchmark/jmh:run -f 1 -wi 10 -i 20 -t 4 -si true .*ImageSerializationBenchmark.*
//

// DEOPSCSW-187: Efficient serialization to/from binary
@State(Scope.Benchmark)
class ImageSerializationBenchmark {
  private var img_32k_Path: Path                 = _
  private var img_32k_Bytes: Array[Byte]         = _
  private var img_128k_Path: Path                = _
  private var img_128k_Bytes: Array[Byte]        = _
  private var img_512k_Path: Path                = _
  private var img_512k_Bytes: Array[Byte]        = _
  private final var system: ActorSystem[_]       = _
  private final var serialization: Serialization = _
  private final var prefixStr: String            = _
  private final var obsId: ObsId                 = _

  @Setup(Level.Trial)
  def setup() = {

    img_32k_Path = Paths.get(getClass.getResource("/images/32k_image.bin").getPath)
    img_32k_Bytes = Files.readAllBytes(img_32k_Path)

    img_128k_Path = Paths.get(getClass.getResource("/images/128k_image.bin").getPath)
    img_128k_Bytes = Files.readAllBytes(img_128k_Path)

    img_512k_Path = Paths.get(getClass.getResource("/images/512k_image.bin").getPath)
    img_512k_Bytes = Files.readAllBytes(img_512k_Path)

    system = ActorSystem(Behaviors.empty, "example")
    serialization = SerializationExtension(system)
    prefixStr = "wfos.prog.cloudcover"
    obsId = ObsId("2020A-001-123")
  }

  @TearDown(Level.Trial)
  def teardown(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def _032kImageSerializationBench(): Array[Byte] = {
    val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make("imageKey")

    val binaryImgData: ArrayData[Byte]    = ArrayData.fromArray(img_32k_Bytes)
    val param: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal

    val observe           = Observe(Prefix("csw.originationPrefix"), CommandName(prefixStr), Some(obsId)).add(param)
    val observeSerializer = serialization.findSerializerFor(observe)

    observeSerializer.toBinary(observe)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def _128kImageSerializationBench(): Array[Byte] = {
    val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make("imageKey")

    val binaryImgData: ArrayData[Byte]    = ArrayData.fromArray(img_128k_Bytes)
    val param: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal

    val observe           = Observe(Prefix("csw.originationPrefix"), CommandName(prefixStr), Some(obsId)).add(param)
    val observeSerializer = serialization.findSerializerFor(observe)

    observeSerializer.toBinary(observe)
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  def _512kImageSerializationBench(): Array[Byte] = {
    val imageKey: Key[ArrayData[Byte]] = ByteArrayKey.make("imageKey")

    val binaryImgData: ArrayData[Byte]    = ArrayData.fromArray(img_512k_Bytes)
    val param: Parameter[ArrayData[Byte]] = imageKey -> binaryImgData withUnits pascal

    val observe           = Observe(Prefix("csw.originationPrefix"), CommandName(prefixStr), Some(obsId)).add(param)
    val observeSerializer = serialization.findSerializerFor(observe)

    observeSerializer.toBinary(observe)
  }
}
