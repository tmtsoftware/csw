import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
import sbt._

object NoPublish extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
}

object PublishBintray extends AutoPlugin {
  import bintray.BintrayPlugin
  import BintrayPlugin.autoImport._

  override def requires: Plugins = BintrayPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    bintrayOrganization := Some("twtmt"),
    bintrayPackage := "csw"
  )
}

object PublishGithub extends AutoPlugin {
  import com.typesafe.sbt.sbtghpages.GhpagesPlugin
  import GhpagesPlugin.autoImport._
  import com.typesafe.sbt.SbtGit.GitKeys

  override def requires: Plugins = GhpagesPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    GitKeys.gitRemoteRepo := "git@github.com:tmtsoftware/csw-prod.git",
    ghpagesNoJekyll := true
  )
}

object DeployApp extends AutoPlugin {
  import com.typesafe.sbt.packager.SettingsHelper
  import com.typesafe.sbt.packager.universal.UniversalPlugin
  import UniversalPlugin.autoImport.{Universal, UniversalDocs}

  override def requires: Plugins = UniversalPlugin && JavaAppPackaging && PublishBintray && CswBuildInfo

  override def projectSettings: Seq[Setting[_]] =
    SettingsHelper.makeDeploymentSettings(Universal, packageBin in Universal, "zip") ++
    SettingsHelper.makeDeploymentSettings(UniversalDocs, packageBin in UniversalDocs, "zip") ++ Seq(
      target in Universal := baseDirectory.value.getParentFile / "target" / "universal"
    )
}

object CswBuildInfo extends AutoPlugin {
  import sbtbuildinfo.BuildInfoPlugin
  import BuildInfoPlugin.autoImport._

  override def requires: Plugins = BuildInfoPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "csw.services"
  )
}
