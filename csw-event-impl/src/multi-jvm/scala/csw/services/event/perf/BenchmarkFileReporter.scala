package csw.services.event.perf

import java.io.{File, OutputStream}
import java.nio.file.Files
import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import com.typesafe.config.Config

/**
 * Simple to file logger for benchmark results. Will log relevant settings first to make sure
 * results can be understood later.
 */
trait BenchmarkFileReporter {
  val fos: OutputStream
  def testName: String
  def reportResults(result: String): Unit
  def close(): Unit
}
object BenchmarkFileReporter {
  val targetDirectory: File = {
    val target = new File("csw-event-impl/target/benchmark-results")
    target.mkdirs()
    target
  }

  val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")

  def apply(test: String, system: ActorSystem, logSettings: Boolean = true): BenchmarkFileReporter =
    new BenchmarkFileReporter {
      override val testName: String = test

      val testResultFile: File = {
        val timestamp = formatter.format(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
        val fileName  = s"$testName-results.txt"
        new File(targetDirectory, fileName)
      }
      val config: Config = system.settings.config

      override val fos: OutputStream = Files.newOutputStream(testResultFile.toPath)

      val settingsToReport =
        Seq(
          "csw.test.EventServicePerfTest.totalMessagesFactor",
          "akka.remote.default-remote-dispatcher.throughput",
          "akka.remote.default-remote-dispatcher.fork-join-executor.parallelism-factor",
          "akka.remote.default-remote-dispatcher.fork-join-executor.parallelism-min",
          "akka.remote.default-remote-dispatcher.fork-join-executor.parallelism-max"
        )
      if (logSettings) settingsToReport.foreach(reportSetting)

      def reportResults(result: String): Unit = synchronized {
        println(result)
        fos.write(result.getBytes("utf8"))
        fos.write('\n')
        fos.flush()
      }

      def reportSetting(name: String): Unit = {
        val value = if (config.hasPath(name)) config.getString(name) else "[unset]"
        reportResults(s"$name: $value")
      }

      def close(): Unit = fos.close()
    }
}
