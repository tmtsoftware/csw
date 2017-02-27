import sbt.Keys._
import sbt._

object UnidocSite extends AutoPlugin {
  import sbtunidoc.{JavaUnidocPlugin, ScalaUnidocPlugin}
  import JavaUnidocPlugin.autoImport._
  import ScalaUnidocPlugin.autoImport._

  import com.typesafe.sbt.site.SitePlugin.autoImport._

  override def requires: Plugins = ScalaUnidocPlugin && JavaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    siteSubdirName in ScalaUnidoc := "api/scala",
    siteSubdirName in JavaUnidoc := "api/java",
    addMappingsToSiteDir(
      mappings in (ScalaUnidoc, packageDoc),
      siteSubdirName in ScalaUnidoc
    ),
    addMappingsToSiteDir(
      mappings in (JavaUnidoc, packageDoc),
      siteSubdirName in JavaUnidoc
    )
  )
}

object ParadoxSite extends AutoPlugin {
  import com.typesafe.sbt.site.paradox.ParadoxSitePlugin
  import ParadoxSitePlugin.autoImport._
  import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport._

  override def requires = ParadoxSitePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    sourceDirectory in Paradox := baseDirectory.value / "src" / "main",
    paradoxProperties in Paradox ++= Map(
      "scaladoc.csw.base_url" -> "https://tmtsoftware.github.io/csw-prod/api/scala",
      "javadoc.csw.base_url" -> "https://tmtsoftware.github.io/csw-prod/api/java"
    )
  )
}
