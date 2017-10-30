import sbt.Keys._
import sbt._

object UnidocSite extends AutoPlugin {
  import sbtunidoc.{BaseUnidocPlugin, JavaUnidocPlugin, ScalaUnidocPlugin}
  import JavaUnidocPlugin.autoImport._
  import ScalaUnidocPlugin.autoImport._
  import BaseUnidocPlugin.autoImport.unidoc

  import com.typesafe.sbt.site.SitePlugin.autoImport._

  override def requires: Plugins = ScalaUnidocPlugin && JavaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    siteSubdirName in ScalaUnidoc := "api/scala",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    siteSubdirName in JavaUnidoc := "api/java",
    filterNotSources(sources in (JavaUnidoc, unidoc), Set("internal", "scaladsl")),
    addMappingsToSiteDir(mappings in (JavaUnidoc, packageDoc), siteSubdirName in JavaUnidoc),
    autoAPIMappings := true
    //      apiURL := Some(url(s"http://tmtsoftware.github.io/csw-prod/api/${version.value}"))
  )

  def filterNotSources(filesKey: TaskKey[Seq[File]], subPaths: Set[String]): Setting[Task[Seq[File]]] = {
    filesKey := filesKey.value.filterNot(file => subPaths.exists(file.getAbsolutePath.contains))
  }
}

object ParadoxSite extends AutoPlugin {
  import com.typesafe.sbt.site.paradox.ParadoxSitePlugin
  import ParadoxSitePlugin.autoImport._
  import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport._

  override def requires: Plugins = ParadoxSitePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    sourceDirectory in Paradox := baseDirectory.value / "src" / "main",
    paradoxTheme := Some("io.github.jonas" % "paradox-material-theme" % "0.2.0"),
    paradoxProperties in Paradox ++= Map(
      "version"                -> version.value,
      "scala.binaryVersion"    -> scalaBinaryVersion.value,
      "scaladoc.base_url"      -> "https://tmtsoftware.github.io/csw-prod/api/scala",
      "javadoc.base_url"       -> "https://tmtsoftware.github.io/csw-prod/api/java",
      "extref.manual.base_url" -> "https://tmtsoftware.github.io/csw-prod/manual/index.html",
      "github.base_url"        -> s"https://github.com/tmtsoftware/csw-prod/tree/master"
    )
  )
}
