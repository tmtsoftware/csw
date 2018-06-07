package csw.services.event.perf.utils

import java.io.{File, PrintWriter}
import java.time.Instant

import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.sys.process
import scala.sys.process.{stringToProcess, FileProcessLogger, ProcessLogger}
import scala.util.Random

trait SystemMonitoringSupport { _: MultiNodeSpec ⇒

  lazy val pid: Int = {
    import java.lang.management._
    val name = ManagementFactory.getRuntimeMXBean.getName
    name.substring(0, name.indexOf('@')).toInt
  }

  val homeDir: String        = System.getProperty("user.home")
  val perfScriptsDir: String = System.getProperty("user.dir") + "/scripts/perf"
  val perfJavaFlamesPath     = s"$homeDir/TMT/perf-map-agent/bin/perf-java-flames"
  val topResultsPath         = s"$homeDir/perf/top_${Instant.now()}.log"
  val jstatResultsPath       = s"$homeDir/perf/jstat/${myself.name}_jstat_${pid}_${Instant.now()}.log"
  val jstatPlotPath          = s"$homeDir/TMT/pritam/jstatplot/target/universal/stage/bin/jstatplot"

  def plotCpuUsageGraph(): process.Process    = executeCmd(s"$perfScriptsDir/cpu_plot.sh $topResultsPath")
  def plotMemoryUsageGraph(): process.Process = executeCmd(s"$perfScriptsDir/memory_plot.sh $topResultsPath")
  def plotLatencyHistogram(inputFilesPath: String, publishFreq: String): process.Process =
    executeCmd(s"$perfScriptsDir/hist_plot.sh $inputFilesPath $publishFreq")

  /**
   * Make sure you have followed below steps before plotting:
   *  1. git clone https://github.com/kpritam/jstatplot.git
   *  2. sbt stage
   *  3. update value of jstatPlotPath from SystemMontoringSupport class with the generated path
   *      ex. $HOME/jstatplot/target/universal/stage/bin/jstatplot
   */
  def plotJstat(): Option[process.Process] = {
    if (exist(jstatPlotPath)) {
      val originalFile = new File(jstatResultsPath)
      val tmpFile      = new File(s"$jstatResultsPath.tmp")
      val w            = new PrintWriter(tmpFile)
      scala.io.Source
        .fromFile(new File(jstatResultsPath))
        .getLines()
        .zipWithIndex
        .map {
          case (line, n) ⇒ if (n == 0) s"Timestamp $line" else s"$n \t\t $line"
        }
        .foreach(w.println)
      w.close()
      tmpFile.renameTo(originalFile)

      Some(executeCmd(s"$jstatPlotPath -m $jstatResultsPath"))
    } else None
  }

  def runTop(): Option[process.Process] = {
    Thread.sleep(Random.nextInt(2) * 1000)
    val process = executeCmd(s"$perfScriptsDir/top.sh $topResultsPath")
    Thread.sleep(1000)
    if (process.isAlive()) Some(process)
    else None
  }

  def runJstat(): Option[FileProcessLogger] = {
    val outFile: File = new File(jstatResultsPath)
    outFile.getParentFile.mkdirs()
    outFile.createNewFile()

    val cmd = s"jstat -gc $pid 1s"
    println(s"[Info] Executing command : [$cmd]")
    val processLogger = ProcessLogger(outFile)
    if (cmd.run(processLogger).isAlive()) Some(processLogger)
    else None
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
      afterDelay onComplete { it ⇒
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

  private def executeCmd(cmd: String): process.Process = {
    println(s"[Info] Executing command : [$cmd]")
    cmd.run(new ProcessLogger {
      override def buffer[T](f: ⇒ T): T   = f
      override def out(s: ⇒ String): Unit = println(s"[Info @ ${myself.name}($pid)] " + s)
      override def err(s: ⇒ String): Unit = println(s"[Error @ ${myself.name}($pid)] " + s)
    })
  }

}
