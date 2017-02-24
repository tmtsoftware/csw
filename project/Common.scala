import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object Common extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = JvmPlugin

  override lazy val projectSettings =
    Seq(
      organization := "org.tmt",
      organizationName := "TMT Org",
      scalaVersion := "2.12.1",

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
      autoAPIMappings := true,

//      apiURL := Some(url(s"http://tmtsoftware.github.io/csw-prod/api/${version.value}")),
      // show full stack traces and test case durations
      testOptions in Test += Tests.Argument("-oDF"),
      // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
      // -a Show stack traces and exception class name for AssertionErrors.
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")
    )
}
