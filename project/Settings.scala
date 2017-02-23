import com.lightbend.paradox.sbt.ParadoxPlugin
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport.{builtinParadoxTheme, paradoxProperties, paradoxTheme}
import com.typesafe.sbt.SbtGit.git
import com.typesafe.sbt.sbtghpages.GhpagesPlugin.autoImport._
import com.typesafe.sbt.site.paradox.ParadoxSitePlugin.autoImport
import sbt.Keys._
import sbt._
import sbtunidoc.Plugin.UnidocKeys.unidoc

object Settings {

  lazy val Local = config("local")

  def docsSettings(p: Project): Seq[Setting[_]] = Seq(
    name := "csw",
    git.remoteRepo := "git@github.com:tmtsoftware/csw-prod.git",
    ghpagesNoJekyll := true,
    paradoxProperties in Local ++= Map(
      // point API doc links to locally generated API docs
      "scaladoc.csw.base_url" -> rebase(
        (baseDirectory in p).value, "../../../../../"
      )((unidoc in p in Compile).value.head).get
    )
  ) ++ Seq(
    inConfig(autoImport.Paradox)(Settings.defaultParadoxSettings),
//    ParadoxPlugin.paradoxSettings(1),
    inConfig(Local)(Settings.defaultParadoxSettings)
  ).flatten

  lazy val defaultParadoxSettings: Seq[Setting[_]] = Seq(
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    paradoxProperties ++= Map(
      "version" -> version.value,
      "scala.binaryVersion" -> scalaBinaryVersion.value,
      "extref.akka-docs.base_url" -> s"http://doc.akka.io/docs/akka/${Akka.Version}/%s.html",
      "extref.java-api.base_url" -> "https://docs.oracle.com/javase/8/docs/api/index.html?%s.html",
      "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/akka/${Akka.Version}",
      "scaladoc.csw.base_url" -> s"http://tmtsoftware.github.io/csw-prod/api/${version.value}"
    ),
    sourceDirectory := baseDirectory.value / "src" / "main"
  )

}
