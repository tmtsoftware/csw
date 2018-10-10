import sbt.Keys._
import sbt._

object UnidocSite extends AutoPlugin {
  import sbtunidoc.{BaseUnidocPlugin, JavaUnidocPlugin, ScalaUnidocPlugin}
  import BaseUnidocPlugin.autoImport.unidoc
  import JavaUnidocPlugin.autoImport._
  import ScalaUnidocPlugin.autoImport._
  import com.typesafe.sbt.site.SitePlugin.autoImport._

  override def requires: Plugins = ScalaUnidocPlugin && JavaUnidocPlugin

  def excludeJavadoc: Set[String] = Set("internal", "scaladsl", "csw_protobuf")
  def excludeScaladoc: String     = Seq("csw_protobuf", "akka").mkString(":")

  override def projectSettings: Seq[Setting[_]] = Seq(
    siteSubdirName in ScalaUnidoc := "/api/scala",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    siteSubdirName in JavaUnidoc := "/api/java",
    filterNotSources(sources in (JavaUnidoc, unidoc), excludeJavadoc),
    addMappingsToSiteDir(mappings in (JavaUnidoc, packageDoc), siteSubdirName in JavaUnidoc),
    scalacOptions in (ScalaUnidoc, unidoc) ++= Seq("-skip-packages", excludeScaladoc),
    autoAPIMappings := true
  )

  def filterNotSources(filesKey: TaskKey[Seq[File]], subPaths: Set[String]): Setting[Task[Seq[File]]] = {
    filesKey := filesKey.value.filterNot(file => subPaths.exists(file.getAbsolutePath.contains))
  }
}

object ParadoxSite extends AutoPlugin {
  import com.typesafe.sbt.site.paradox.ParadoxSitePlugin
  import ParadoxSitePlugin.autoImport._
  import _root_.io.github.jonas.paradox.material.theme.ParadoxMaterialThemePlugin
  import ParadoxMaterialThemePlugin.autoImport._
  import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport._

  override def requires: Plugins = ParadoxSitePlugin && ParadoxMaterialThemePlugin

  override def projectSettings: Seq[Setting[_]] =
    ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox) ++
    Seq(
      sourceDirectory in Paradox := baseDirectory.value / "src" / "main",
      sourceDirectory in (Paradox, paradoxTheme) := (sourceDirectory in Paradox).value / "_template",
      paradoxMaterialTheme in Paradox ~= {
        _.withFavicon("assets/tmt_favicon.ico")
          .withRepository(new URI("https://github.com/tmtsoftware/csw"))
      },
      paradoxProperties in Paradox ++= Map(
        "version"                -> version.value,
        "scala.binaryVersion"    -> scalaBinaryVersion.value,
        "scaladoc.base_url"      -> s"https://tmtsoftware.github.io/csw/${version.value}/api/scala",
        "javadoc.base_url"       -> s"https://tmtsoftware.github.io/csw/${version.value}/api/java",
        "extref.manual.base_url" -> s"https://tmtsoftware.github.io/csw/${version.value}/manual/index.html",
        "github.base_url"        -> "https://github.com/tmtsoftware/csw/tree/master"
      )
    )
}
