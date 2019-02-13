import java.io.File

import com.typesafe.sbt.packager.universal.ZipHelper
import sbt.Keys._
import sbt.io.Path
import sbt.{AutoPlugin, Def, ProjectReference, Task, taskKey, _}

import scala.language.postfixOps
import scala.sys.process._


object CoursierArtifactGenerator extends AutoPlugin {

  val runStageWithCoursier = taskKey[Unit]("Stage with coursier bootstrap script.")

  def generateArtifacts(projects: Seq[ProjectReference]): Def.Setting[Task[Unit]] = {
    runStageWithCoursier := {
      projects
        .map(p ⇒ publishLocal in p)
        .join
        .value
      val dir = baseDirectory.value / "scripts"
      s"sh $dir/csw-bootstrap.sh ${version.value} ${baseDirectory.value}" !
    }
  }

  def zipCoursierArtifactsTask(projects: Seq[ProjectReference]): Def.Initialize[Task[File]] = Def.task {
    val ghrleaseDir = target.value / "ghrelease"
    val zipFileName = s"csw-bootstrap-apps-${version.value}"
    lazy val appsZip = new File(ghrleaseDir, s"$zipFileName.zip")

    generateArtifacts(projects).init.value

    val tuples = Path.allSubpaths(target.value / "coursier/stage")
    val mapping = tuples.map {
      case (source, dest) ⇒ (source, s"$zipFileName/$dest")
    }

    ZipHelper.zipNative(mapping, appsZip)
    appsZip
  }

}
