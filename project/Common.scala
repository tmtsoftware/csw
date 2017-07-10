
import Libs._
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import com.github.tototoshi.sbt.buildfileswatcher.Plugin
import com.typesafe.sbt.SbtGit.GitCommand

object Common extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  override lazy val projectSettings: Seq[Setting[_]] = extraSettings ++ scalafmtSettings ++ Seq(
    organization := "org.tmt",
    organizationName := "TMT Org",
    scalaVersion := Libs.ScalaVersion,

    concurrentRestrictions in Global += Tags.limit(Tags.All, 1),

    homepage := Some(url("https://github.com/tmtsoftware/csw-prod")),
    scmInfo := Some(ScmInfo(url("https://github.com/tmtsoftware/csw-prod"), "git@github.com:tmtsoftware/csw-prod.git")),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),

    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      //"-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture"
    ),

    javacOptions ++= Seq(
      //        "-Xlint:unchecked"
    ),

    testOptions in Test ++= Seq(
      // show full stack traces and test case durations
      Tests.Argument("-oDF"),
      // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
      // -a Show stack traces and exception class name for AssertionErrors.
      Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
    ),

    shellPrompt := { state =>
      Plugin.messageOnBuildFilesChanged(state) + GitCommand.prompt(state)
    },

    version := {
      sys.props.get("prod.publish") match {
        case Some("true") => version.value
        case _            => "0.1-SNAPSHOT"
      }
    },

    isSnapshot := sys.props.get("prod.publish") != Some("true")
  )

  private def extraSettings = sys.props.get("check.cycles") match {
    case Some("true") => Seq(
      libraryDependencies += `acyclic`,
      autoCompilerPlugins := true,
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.7"),
      scalacOptions += "-P:acyclic:force"
    )
    case _            =>
      List.empty
  }


  // After upgrading from 0.6.6 to 0.6.8 scalafmt downloads stuff after every change to the project
  // This is a workaround from github issue : https://github.com/scalameta/scalafmt/issues/879
  private def scalafmtSettings() = {
    def latestScalafmt = "0.6.8"

    commands += Command.args("scalafmt", "Run scalafmt cli.") {
      case (state, args) =>
        val Right(scalafmt) =
          org.scalafmt.bootstrap.ScalafmtBootstrap.fromVersion(latestScalafmt)
        scalafmt.main("--non-interactive" +: args.toArray)
        state
    }
  }
}
