import Libs._
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Common extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  val detectCycles: SettingKey[Boolean] = settingKey[Boolean]("is cyclic check enabled?")

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    organization := "org.tmt",
    organizationName := "TMT Org",
    scalaVersion := Libs.ScalaVersion,
    concurrentRestrictions in Global += Tags.limit(Tags.All, 1),
    homepage := Some(url("https://github.com/tmtsoftware/csw-prod")),
    scmInfo := Some(
      ScmInfo(url("https://github.com/tmtsoftware/csw-prod"), "git@github.com:tmtsoftware/csw-prod.git")
    ),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      //"-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture",
      if (cycleCheckEnabled && detectCycles.value) "-P:acyclic:force" else ""
    ),
    javacOptions in (Compile, doc) ++= Seq("-Xdoclint:none"),
    testOptions in Test ++= Seq(
      // show full stack traces and test case durations
      Tests.Argument("-oDF"),
      // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
      // -a Show stack traces and exception class name for AssertionErrors.
      Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
    ),
    version := {
      sys.props.get("prod.publish") match {
        case Some("true") => version.value
        case _            => "0.1-SNAPSHOT"
      }
    },
    isSnapshot := !sys.props.get("prod.publish").contains("true"),
    fork := true,
    detectCycles := true,
    libraryDependencies += `acyclic`,
    autoCompilerPlugins := true,
    addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7")
  )

  private def cycleCheckEnabled = sys.props.get("check.cycles") match {
    case Some("true") => true
    case _            => false
  }
}
