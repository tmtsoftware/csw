import java.io.File

import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys.MultiJvm
import sbt.Keys._
import sbt._
import sbt.io.Path

object Settings {

  def addAliases: Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      "test; multi-jvm:test"
    ) ++
    addCommandAlias(
      "compileAll",
      "; scalafmtCheck; scalastyle; test:compile; multi-jvm:compile"
    ) ++
    addCommandAlias(
      "buildAll",
      "; scalafmtCheck; scalastyle; clean; makeSite; test:compile; multi-jvm:compile"
    )
  }

  def multiJvmTestTask(multiJvmProjects: Seq[ProjectReference]): Seq[Setting[_]] = {
    val tasks: Seq[Def.Initialize[Task[Unit]]] = multiJvmProjects.map(p => p / MultiJvm / test)

    Seq(
      MultiJvm / test / aggregate := false,
      MultiJvm / test             := Def.sequential(tasks).value
    )
  }

  // export ESW_TS_VERSION env variable which is compatible with csw
  // this represents version number of javascript docs maintained at https://github.com/tmtsoftware/esw-ts
  def eswTsVersion: String =
    (sys.env ++ sys.props).get("ESW_TS_VERSION") match {
      case Some(v) => v
      case None    => "0.1.0-SNAPSHOT"
    }

  def addLoggingAggregator: Def.Initialize[Task[File]] = {
    Def.task {
      val ghReleaseDir = target.value / "ghrelease"
      val zipFileName  = s"logging-aggregator-${version.value}"
      lazy val appsZip = new File(ghReleaseDir, s"$zipFileName.zip")

      val scriptsDir = file(".") / "scripts"
      val loggingAggregator = Path
        .directory(new File(scriptsDir, "logging_aggregator"))
        .filterNot { case (_, s) => s.startsWith("logging_aggregator/prod") }

      sbt.IO.zip(loggingAggregator, appsZip, None)
      appsZip
    }
  }
}
