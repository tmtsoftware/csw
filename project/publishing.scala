import sbt.Keys._
import sbt._

object NoPublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def projectSettings = Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
}

object PublishBintray extends AutoPlugin {
  import bintray.BintrayPlugin
  import BintrayPlugin.autoImport._

  override def requires = BintrayPlugin

  override def projectSettings = Seq(
    bintrayOrganization := Some("twtmt"),
    bintrayPackage := "csw"
  )
}

object PublishGithub extends AutoPlugin {
  import com.typesafe.sbt.sbtghpages.GhpagesPlugin
  import GhpagesPlugin.autoImport._
  import com.typesafe.sbt.SbtGit.GitKeys

  override def requires = GhpagesPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    GitKeys.gitRemoteRepo := "git@github.com:tmtsoftware/csw-prod.git",
    ghpagesNoJekyll := true
  )
}
