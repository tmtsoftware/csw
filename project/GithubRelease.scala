import java.io.File

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.ZipHelper
import ohnosequences.sbt.GithubRelease.keys.{ghreleaseAssets, ghreleaseRepoName, ghreleaseRepoOrg, githubRelease}
import ohnosequences.sbt.SbtGithubReleasePlugin
import sbt.Keys._
import sbt.io.{IO, Path}
import sbt.{AutoPlugin, Def, Plugins, ProjectReference, Setting, Task, taskKey, _}

object GithubRelease extends AutoPlugin {

  val coverageReportZipKey = taskKey[File]("Creates a distributable zip file containing the coverage report.")
  val testReportZipKey     = taskKey[File]("Creates a distributable zip file containing the test reports.")

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(Compile))

  override def requires: Plugins = SbtGithubReleasePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    ghreleaseRepoOrg := "tmtsoftware",
    ghreleaseRepoName := "csw-prod",
    aggregate in githubRelease := false,
    // this creates scoverage report zip file and required for GithubRelease task, it assumes that scoverage-report is already generated
    // and is available inside target folder (if it is not present, empty zip will be created)
    coverageReportZipKey := coverageReportZipTask.value,
    testReportZipKey := testReportsZipTask.value
  )

  private def coverageReportZipTask = Def.task {
    lazy val coverageReportZip = new File(target.value / "ghrelease", "scoverage-report.zip")
    IO.zip(Path.allSubpaths(new File(crossTarget.value, "scoverage-report")), coverageReportZip)
    coverageReportZip
  }

  private def testReportsZipTask = Def.task {
    lazy val testReportsZip = new File(target.value / "ghrelease", "test-reports.zip")
    val testXmlReportFiles  = target.all(aggregateFilter).value flatMap (x ⇒ Path.allSubpaths(x / "test-reports"))
    IO.zip(testXmlReportFiles, testReportsZip)
    testReportsZip
  }

  private def stageAndZipTask(projects: Seq[ProjectReference]): Def.Initialize[Task[File]] = Def.task {
    lazy val appsZip  = new File(target.value / "ghrelease", s"csw-apps-${version.value}.zip")
    val serviceScript = baseDirectory.value / "scripts" / "csw-services.sh"

    val stagedFiles = projects
      .map(p ⇒ stage in Universal in p)
      .join
      .value
      .flatMap(x ⇒ Path.allSubpaths(x))
      .distinct :+ ((serviceScript, s"bin/${serviceScript.getName}"))

    ZipHelper.zipNative(stagedFiles, appsZip)
    appsZip
  }

  def githubReleases(projects: Seq[ProjectReference]): Setting[Task[Seq[sbt.File]]] =
    ghreleaseAssets := Seq(
      stageAndZipTask(projects).value,
      coverageReportZipKey.value,
      testReportZipKey.value
    )

}
