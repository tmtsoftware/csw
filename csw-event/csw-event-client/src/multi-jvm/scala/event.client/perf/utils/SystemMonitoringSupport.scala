package csw.event.client.perf.utils

import java.io.{File, PrintWriter}
import java.lang.ProcessBuilder.Redirect
import java.time.Instant

import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Random

trait SystemMonitoringSupport { multiNodeSpec: MultiNodeSpec =>

  lazy val pid: Int = {
    import java.lang.management._
    val name = ManagementFactory.getRuntimeMXBean.getName
    name.substring(0, name.indexOf('@')).toInt
  }

  val homeDir: String        = System.getProperty("user.home")
  val perfScriptsDir: String = "/home/centos/project/csw/scripts/perf"
  val perfJavaFlamesPath     = s"$homeDir/TMT/perf-map-agent/bin/perf-java-flames"
  val topResultsPath         = s"$homeDir/perf/top_${Instant.now()}.log"
  val jstatResultsPath       = s"$homeDir/perf/jstat/${myself.name}_jstat_${pid}_${Instant.now()}.log"
  val jstatPlotPath          = s"$homeDir/project/jstatplot/target/universal/stage/bin/jstatplot"

  def plotCpuUsageGraph(): Process    = executeCmd(s"$perfScriptsDir/cpu_plot.sh", topResultsPath)
  def plotMemoryUsageGraph(): Process = executeCmd(s"$perfScriptsDir/memory_plot.sh", topResultsPath)
  def plotLatencyHistogram(inputFilesPath: String, publishFreq: String): Process =
    executeCmd(s"$perfScriptsDir/hist_plot.sh", inputFilesPath, publishFreq)

  /**
   * Make sure you have followed below steps before plotting:
   *  1. git clone https://github.com/kpritam/jstatplot.git
   *  2. sbt stage
   *  3. update value of jstatPlotPath from SystemMontoringSupport class with the generated path
   *      ex. $HOME/jstatplot/target/universal/stage/bin/jstatplot
   */
  def plotJstat(): Option[Process] = {
    if (exist(jstatPlotPath)) {
      val originalFile = new File(jstatResultsPath)
      val tmpFile      = new File(s"$jstatResultsPath.tmp")
      val w            = new PrintWriter(tmpFile)
      scala.io.Source
        .fromFile(new File(jstatResultsPath))
        .getLines()
        .zipWithIndex
        .map {
          case (line, n) => if (n == 0) s"Timestamp $line" else s"$n \t\t $line"
        }
        .foreach(w.println)
      w.close()
      tmpFile.renameTo(originalFile)

      Some(executeCmd(s"$jstatPlotPath", "-m", s"$jstatResultsPath"))
    } else None
  }

  def runTop(): Option[Process] = {
    Thread.sleep(Random.nextInt(2) * 1000)
    val process = executeCmd(s"$perfScriptsDir/top.sh", topResultsPath)

    Thread.sleep(1000)
    if (process.isAlive) Some(process)
    else None
  }

  def runJstat(): Unit = {
    val outFile: File = new File(jstatResultsPath)
    outFile.getParentFile.mkdirs()
    outFile.createNewFile()

    val cmd = s"jstat -gc $pid 1s"
    println(s"[Info] Executing command : [$cmd]")

    val redirectFile = Redirect.to(outFile)

    new ProcessBuilder("jstat", "-gc", pid.toString, "1s")
      .redirectErrorStream(true)
      .redirectOutput(redirectFile)
      .start
  }

  /**
   * Runs `perf-java-flames` script on given node (JVM process).
   * Refer to https://github.com/jrudolph/perf-map-agent for options and manual.
   *
   * Options are currently to be passed in via `export PERF_MAP_OPTIONS` etc.
   */
  def runPerfFlames(nodes: RoleName*)(delay: FiniteDuration, time: FiniteDuration = 15.seconds): Unit = {
    if (exist(perfJavaFlamesPath) && isNode(nodes: _*)) {
      import scala.concurrent.ExecutionContext.Implicits.global

      val afterDelay = akka.pattern.after(delay, system.scheduler)(Future.successful("GO!"))
      afterDelay onComplete { it =>
        val perfCommand = s"$perfJavaFlamesPath $pid"
        println(s"[perf @ ${myself.name}($pid)][OUT]: " + perfCommand)

        executeCmd(perfCommand)
      }
    }
  }

  private def exist(file: String): Boolean = {
    val isIt = new File(file).exists()
    if (!isIt) println(s"[Warning] $file does not exist.")
    isIt
  }

  private def executeCmd(cmd: String*): Process = {
    println(s"[Info] Executing command : [${cmd.mkString(" ")}]")
    val process = new ProcessBuilder(cmd: _*).start()
    import multiNodeSpec.system.dispatcher

    Future {
      Source
        .fromInputStream(process.getErrorStream())
        .getLines()
        .foreach(line => println(s"[Error @ ${myself.name}($pid)] " + line))

      Source
        .fromInputStream(process.getInputStream())
        .getLines()
        .foreach(line => println(s"[Info @ ${myself.name}($pid)] " + line))
    }

    process
  }

}
