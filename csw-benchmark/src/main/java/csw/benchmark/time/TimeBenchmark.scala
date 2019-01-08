package csw.benchmark.time

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.infra.Blackhole
import org.openjdk.jmh.annotations._

@Warmup(iterations = 4)
@Measurement(iterations = 10)
class TimeBenchmark {

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  @Fork(jvmArgs = Array("-Djava.library.path=src/main/java/csw/benchmark/time"))
  def testMethodJNI(b: Blackhole): Unit = b.consume(new TimeJNI().gettime)

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def testMethodJNA(b: Blackhole): Unit = b.consume(TimeLibrary.clock_gettime(0, new TimeSpec))
}
