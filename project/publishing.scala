import java.io.File

import ParadoxSite.docsParentDir
import sbt.Keys._
import sbt.io.Path
import sbt.{Def, _}

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

object GithubPublishDocs extends AutoPlugin {
  import com.typesafe.sbt.SbtGit.GitKeys
  import com.typesafe.sbt.sbtghpages.GhpagesPlugin
  import GhpagesPlugin.autoImport._

  override def requires: Plugins = GhpagesPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    ghpagesBranch := "master",
    includeFilter in ghpagesCleanSite := new FileFilter {
      override def accept(pathname: File): Boolean = pathname.getAbsolutePath.contains(s"$docsParentDir/${version.value}")
    },
    GitKeys.gitRemoteRepo := "git@github.com:tmtsoftware/tmtsoftware.github.io.git"
  )
}

object DeployApp extends AutoPlugin {
  import com.typesafe.sbt.packager.SettingsHelper
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
  import com.typesafe.sbt.packager.universal.UniversalPlugin
  import UniversalPlugin.autoImport.{Universal, UniversalDocs}

  override def requires: Plugins = UniversalPlugin && JavaAppPackaging && PublishBintray && CswBuildInfo

  override def projectSettings: Seq[Setting[_]] =
    SettingsHelper.makeDeploymentSettings(Universal, packageBin in Universal, "zip") ++
    SettingsHelper.makeDeploymentSettings(UniversalDocs, packageBin in UniversalDocs, "zip") ++ Seq(
      target in Universal := file(".") / "target" / "universal",
      mappings in Universal := (mappings in Universal).value ++ scriptsAndConfsMapping.value
    )

  private def scriptsAndConfsMapping = Def.task {
    val scriptsDir       = file(".") / "scripts"
    val sentinelConf     = scriptsDir / "conf" / "redis_sentinel" / "sentinel.conf"
    val authServerDir    = scriptsDir / "csw-auth" / "prod"
    val serviceScript    = scriptsDir / "csw-services.sh"
    val prodScript       = scriptsDir / "redis-sentinel-prod.sh"
    val authServerScript = authServerDir / "configure.sh"
    val confs = Path
      .directory(new File(scriptsDir, "conf"))
      .filterNot { case (f, s) => s.equals("conf/redis_sentinel/sentinel.conf") }

    confs :+
    ((serviceScript, s"bin/${serviceScript.getName}")) :+
    ((prodScript, s"bin/${prodScript.getName}")) :+
    ((authServerScript, s"bin/${authServerScript.getName}")) :+
    ((sentinelConf, s"conf/redis_sentinel/sentinel-template.conf"))
  }
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
