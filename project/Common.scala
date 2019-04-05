import Libs._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{testOptions, _}
import sbt._
import sbt.plugins.JvmPlugin
import sbtunidoc.GenJavadocPlugin.autoImport.unidocGenjavadocVersion

object Common extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = JvmPlugin

  val suppressAnnotatedWarnings: SettingKey[Boolean] = settingKey[Boolean]("enable annotation based suppression of warnings")

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    organization := "com.github.tmtsoftware.csw",
    organizationName := "TMT Org",
    scalaVersion := Libs.ScalaVersion,
    concurrentRestrictions in Test += Tags.limit(Tags.All, 1),
    homepage := Some(url("https://github.com/tmtsoftware/csw")),
    resolvers += "jitpack" at "https://jitpack.io",
    resolvers += "bintray" at "http://jcenter.bintray.com",
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
//      "-Xfatal-warnings",
      "-Xlint:_,-missing-interpolator",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Xfuture",
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
    libraryDependencies ++= Seq(`silencer-lib`),
    libraryDependencies ++= (if (suppressAnnotatedWarnings.value) Seq(compilerPlugin(`silencer-plugin`)) else Seq.empty),
    autoCompilerPlugins := true,
    cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
    if (formatOnCompile) scalafmtOnCompile := true else scalafmtOnCompile := false,
    unidocGenjavadocVersion := "0.13"
  )

  private def formatOnCompile = sys.props.get("format.on.compile") match {
    case Some("false") ⇒ false
    case _             ⇒ true
  }
}
