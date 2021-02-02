import com.typesafe.sbt.site.SitePlugin.autoImport.siteDirectory
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import org.tmt.sbt.docs.DocKeys.{docsParentDir, docsRepo, gitCurrentRepo}
import sbt.Keys._
import sbt._
import sbtunidoc.GenJavadocPlugin.autoImport.unidocGenjavadocVersion

object Common {
  private val enableFatalWarnings: Boolean = sys.props.get("enableFatalWarnings").contains("true")
  private val enableCoverage: Boolean      = sys.props.get("enableCoverage").contains("true")
  val storyReport: Boolean                 = sys.props.get("generateStoryReport").contains("true")

  private val reporterOptions: Seq[Tests.Argument] =
    // "-oDF" - show full stack traces and test case durations
    // -C - to give fully qualified name of the custom reporter
    if (storyReport)
      Seq(
        Tests.Argument(TestFrameworks.ScalaTest, "-oDF", "-C", "tmt.test.reporter.TestReporter")
      )
    else Seq(Tests.Argument("-oDF"))

  val MaybeCoverage: Plugins = if (enableCoverage) Coverage else Plugins.empty
  val jsTestArg              = testOptions in Test := Seq(Tests.Argument("-oDF"))

  lazy val CommonSettings: Seq[Setting[_]] = Seq(
    docsRepo := "https://github.com/tmtsoftware/tmtsoftware.github.io.git",
    docsParentDir := "csw",
    gitCurrentRepo := "https://github.com/tmtsoftware/csw",
    libraryDependencies += (Libs.`tmt-test-reporter` % Test),
    organization := "com.github.tmtsoftware.csw",
    organizationName := "TMT Org",
    scalaVersion := Libs.ScalaVersion,
    homepage := Some(url("https://github.com/tmtsoftware/csw")),
    resolvers ++= Seq(
      "jitpack" at "https://jitpack.io",
      "bintray" at "https://jcenter.bintray.com"
    ),
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
      //-W Options
      "-Wdead-code",
      if (enableFatalWarnings) "-Wconf:any:error" else "-Wconf:any:warning-verbose",
      //-X Options
      "-Xlint:_,-missing-interpolator",
      "-Xsource:3",
      "-Xcheckinit",
      "-Xasync"
      // -Y options are rarely needed, please look for -W equivalents
    ),
    javacOptions in (Compile, doc) ++= Seq("-Xdoclint:none"),
    javacOptions in doc ++= Seq("--ignore-source-errors"),
    testOptions in Test ++= reporterOptions,
    publishArtifact in (Test, packageBin) := true,
    version := {
      sys.props.get("prod.publish") match {
        case Some("true") => version.value
        case _            => "0.1.0-SNAPSHOT"
      }
    },
    isSnapshot := !sys.props.get("prod.publish").contains("true"),
    fork := true,
    javaOptions in Test ++= Seq("-Dakka.actor.serialize-messages=on"),
    autoCompilerPlugins := true,
    cancelable in Global := true, // allow ongoing test(or any task) to cancel with ctrl + c and still remain inside sbt
    scalafmtOnCompile := true,
    unidocGenjavadocVersion := "0.16",
    commands += Command.command("openSite") { state =>
      val uri = s"file://${Project.extract(state).get(siteDirectory)}/${docsParentDir.value}/${version.value}/index.html"
      state.log.info(s"Opening browser at $uri ...")
      java.awt.Desktop.getDesktop.browse(new java.net.URI(uri))
      state
    },
    Global / excludeLintKeys := Set(
      SettingKey[Boolean]("ide-skip-project"),
      aggregate //verify if this needs to be here or our configuration is wrong
    )
  )
}
