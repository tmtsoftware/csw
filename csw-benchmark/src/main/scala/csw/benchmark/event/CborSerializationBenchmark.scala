/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.benchmark.event

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import csw.params.core.formats.EventCbor
import csw.params.events.SystemEvent
import org.openjdk.jmh.annotations.*

// RUN using this command: csw-benchmark/jmh:run -f 1 -wi 5 -i 5 csw.benchmark.event.CborSerializationBenchmark
@State(Scope.Benchmark)
class CborSerializationBenchmark {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def cborAvgTime(): SystemEvent = {
    val bytes: Array[Byte] = EventCbor.encode(Data.bigEvent)
    EventCbor.decode[SystemEvent](bytes)
  }

}

object BigCborTest {
  def main(args: Array[String]): Unit = {
    val bytes: Array[Byte] = EventCbor.encode(Data.bigEvent)
    val event              = EventCbor.decode[SystemEvent](bytes)
    assert(event == Data.bigEvent)
    println(bytes.length)
  }
}

object CrossLanguageCbor {
  def main(args: Array[String]): Unit = {

    val bytes: Array[Byte] = EventCbor.encode(Data.smallEvent)

    val bos = new BufferedOutputStream(new FileOutputStream("/tmp/input.cbor"))
    bos.write(bytes)
    bos.close()

    val readBytes = Files.readAllBytes(Paths.get("/tmp/input.cbor"))
    val event     = EventCbor.decode[SystemEvent](readBytes)
    println(event)
  }
}
