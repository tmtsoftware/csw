import java.io.File

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import com.typesafe.sbt.packager.universal.ZipHelper
import sbt.Keys._
import sbt.io.{IO, Path}
import sbt.{Def, ProjectReference, Task, _}

object GithubReleaseSettings {

  def stageAndZipTask(projects: Seq[ProjectReference]): Def.Initialize[Task[File]] =
    Def.task {
      val ghrleaseDir = target.value / "ghrelease"
      val log         = sLog.value
      val zipFileName = s"csw-apps-${version.value}"

      lazy val appsZip = new File(ghrleaseDir, s"$zipFileName.zip")

      log.info("Deleting staging directory ...")
      // delete older files from staging directory to avoid getting it included in zip
      // in order to delete directory first and then stage projects, below needs to be a task
      val () = Def.task {
        IO.delete(target.value / "universal" / "stage")
      }.value

      log.info(s"Staging projects: [${projects.mkString(" ,")}]")
      val stagedFiles = projects
        .map(p => stage in Universal in p)
        .join
        .value
        .flatMap(x => Path.allSubpaths(x.getAbsoluteFile))
        .distinct
        .map {
          case (source, dest) => (source, s"$zipFileName/$dest")
        }

      ZipHelper.zipNative(stagedFiles, appsZip)
      appsZip
    }


}