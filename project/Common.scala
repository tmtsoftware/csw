import Libs._
import com.timushev.sbt.updates.UpdatesPlugin.autoImport.dependencyUpdatesFilter
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbtunidoc.GenJavadocPlugin.autoImport.unidocGenjavadocVersion

object Common extends AutoPlugin {

  // enable these values to be accessible to get and set in sbt console
  object autoImport {
    val suppressAnnotatedWarnings: SettingKey[Boolean] = settingKey[Boolean]("enable annotation based suppression of warnings")
    val enableFatalWarnings: SettingKey[Boolean]       = settingKey[Boolean]("enable fatal warnings")
  }

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  import autoImport._
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    dependencyOverrides += AkkaHttp.`akka-http-spray-json`,
    organization := "com.github.tmtsoftware.csw",
    organizationName := "TMT Org",
    scalaVersion := Libs.ScalaVersion,
    homepage := Some(url("https://github.com/tmtsoftware/csw")),
    resolvers += "jitpack" at "https://jitpack.io",
    resolvers += "bintray" at "https://jcenter.bintray.com",
    scmInfo := Some(
      ScmInfo(url("https://github.com/tmtsoftware/csw"), "git@github.com:tmtsoftware/csw.git")
    ),
    licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      if (enableFatalWarnings.value) "-Xfatal-warnings" else "",
      "-Xlint:_,-missing-interpolator",
      "-Ywarn-dead-code",
//      "-Xprint:typer"
      if (suppressAnnotatedWarnings.value) s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}" else ""
    ),
    javacOptions in (Compile, doc) ++= Seq("-Xdoclint:none"),
    javacOptions in doc ++= Seq("--ignore-source-errors"),
    testOptions in Test ++= Seq(
      // show full stack traces and test case durations
      Tests.Argument("-oDF")
    ),
    publishArtifact in (Test, packageBin) := true,
    version := {
      sys.props.get("prod.publish") match {
        case Some("true") => version.value
        case _            => "0.1-SNAPSHOT"
      }
    },
    isSnapshot := !sys.props.get("prod.publish").contains("true"),
    fork := true,
    suppressAnnotatedWarnings := true,
    enableFatalWarnings := false,
    libraryDependencies ++= Seq(`silencer-lib`),
    libraryDependencies ++= (if (suppressAnnotatedWarnings.value) Seq(compilerPlugin(`silencer-plugin`)) else Seq.empty),
    autoCompilerPlugins := true,
    cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
    scalafmtOnCompile := true,
    unidocGenjavadocVersion := "0.13",
    dependencyUpdatesFilter := dependencyUpdatesFilter.value - moduleFilter(organization = "org.scala-lang")
  )
}
