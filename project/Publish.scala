import sbt.Keys._
import sbt._

/**
 * For projects that are not to be published.
 */
object NoPublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def projectSettings = Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
}

object Publish extends AutoPlugin {
  import bintray.BintrayPlugin
  import BintrayPlugin.autoImport._

  override def requires = BintrayPlugin

  override def projectSettings = Seq(
    bintrayOrganization := Some("twtmt"),
    bintrayPackage := "csw"
  )
}

object PublishUnidoc extends AutoPlugin {
  import sbtunidoc.{JavaUnidocPlugin, ScalaUnidocPlugin}
  import ScalaUnidocPlugin.autoImport._
  import JavaUnidocPlugin.autoImport._
  import com.typesafe.sbt.site.SitePlugin.autoImport._

  override def requires = ScalaUnidocPlugin && JavaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    siteSubdirName in ScalaUnidoc := "latest/api",
    siteSubdirName in JavaUnidoc := "java/api",
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

object PublishParadox extends AutoPlugin {
  import com.typesafe.sbt.site.paradox.ParadoxSitePlugin
  import ParadoxSitePlugin.autoImport._
  import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport._

  override def requires = ParadoxSitePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    sourceDirectory in Paradox := baseDirectory.value / "src" / "main",
    paradoxProperties in Paradox ++= Map(
      "extref.rfc.base_url" -> "http://tools.ietf.org/html/rfc%s"
    )
  )
}