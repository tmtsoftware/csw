import sbt._
import Keys._
import sbtunidoc.BaseUnidocPlugin.autoImport
import sbtunidoc.{JavaUnidocPlugin, ScalaUnidocPlugin}

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
  import bintray.BintrayPlugin.autoImport._

  override def requires = BintrayPlugin

  override def projectSettings = Seq(
    bintrayOrganization := Some("twtmt"),
    bintrayPackage := "csw"
  )
}

object PublishUnidoc extends AutoPlugin {
  import com.typesafe.sbt.SbtGit.GitKeys._
  import ScalaUnidocPlugin.autoImport._
  import com.typesafe.sbt.site.SitePlugin.autoImport._

  override def requires = ScalaUnidocPlugin && JavaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    siteSubdirName in ScalaUnidoc := "latest/api",
    siteSubdirName in JavaUnidocPlugin.autoImport.JavaUnidoc := "java/api",
    addMappingsToSiteDir(
      mappings in (ScalaUnidoc, packageDoc),
      siteSubdirName in ScalaUnidoc
    ),
    addMappingsToSiteDir(
      mappings in (JavaUnidocPlugin.autoImport.JavaUnidoc, packageDoc),
      siteSubdirName in JavaUnidocPlugin.autoImport.JavaUnidoc
    ),
    gitRemoteRepo := "git@github.com:tmtsoftware/csw-prod.git"
  )
}
