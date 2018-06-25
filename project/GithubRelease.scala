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
    val log                    = sLog.value
    val zipFileName            = s"csw-apps-${version.value}"
    lazy val appsZip           = new File(target.value / "ghrelease", s"$zipFileName.zip")
    val scriptsDir             = baseDirectory.value / "scripts"
    val serviceScript          = scriptsDir / "csw-services.sh"
    val eventServiceDir        = scriptsDir / "event_service"
    val eventServiceProdScript = eventServiceDir / "event_service_sentinel_prod.sh"
    val eventServiceConfs      = Path.allSubpaths(new File(eventServiceDir, "conf"))

    log.info("Deleting staging directory ...")
    // delete older files from staging directory to avoid getting it included in zip
    // in order to delete directory first and then stage projects, below needs to be a task
    Def.task {
      IO.delete(target.value / "universal" / "stage")
    }.value

    log.info(s"Staging projects: [${projects.mkString(" ,")}]")
    val stagedFiles = projects
      .map(p ⇒ stage in Universal in p)
      .join
      .value
      .flatMap(x ⇒ Path.allSubpaths(x))
      .distinct
      .map {
        case (source, dest) ⇒ (source, s"$zipFileName/$dest")
      } ++
    eventServiceConfs.map { case (source, dest) ⇒ (source, s"$zipFileName/conf/$dest") } :+
    ((serviceScript, s"$zipFileName/bin/${serviceScript.getName}")) :+
    ((eventServiceProdScript, s"$zipFileName/bin/${eventServiceProdScript.getName}"))

    ZipHelper.zipNative(stagedFiles, appsZip)
    appsZip
  }

  def githubReleases(projects: Seq[ProjectReference]): Setting[Task[Seq[sbt.File]]] =
    ghreleaseAssets := Seq(
      stageAndZipTask(projects).value
    )

}
