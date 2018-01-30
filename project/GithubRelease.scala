import java.io.File

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal
import ohnosequences.sbt.GithubRelease.keys.{ghreleaseAssets, ghreleaseRepoName, ghreleaseRepoOrg, githubRelease}
import ohnosequences.sbt.SbtGithubReleasePlugin
import sbt.Keys.{aggregate, crossTarget, packageBin}
import sbt.io.{IO, Path}
import sbt.{AutoPlugin, Plugins, ProjectReference, Setting, Task, TaskKey}

object GithubRelease extends AutoPlugin {

  val coverageReportZipKey = TaskKey[File]("coverage-zip", "Creates a distributable zip file containing the coverage report.")

  override def requires: Plugins = SbtGithubReleasePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    ghreleaseRepoOrg := "tmtsoftware",
    ghreleaseRepoName := "csw-prod",
    aggregate in githubRelease := false,
    // this creates scoverage report zip file and required for GithubRelease task, it assumes that scoverage-report is already generated
    // and is available inside target folder (if it is not present, empty zip will be created)
    coverageReportZipKey := {
      lazy val coverageReportZip = new File(crossTarget.value, "scoverage-report.zip")
      IO.zip(Path.allSubpaths(new File(crossTarget.value, "scoverage-report")), coverageReportZip)
      coverageReportZip
    }
  )

  def githubReleases(projects: Seq[ProjectReference]): Setting[Task[Seq[sbt.File]]] =
    ghreleaseAssets := projects.map(p ⇒ packageBin in Universal in p).join.value :+ coverageReportZipKey.value

}
