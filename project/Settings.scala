import ParadoxSite.docsParentDir
import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys.MultiJvm
import com.typesafe.sbt.site.SitePlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtunidoc.BaseUnidocPlugin.autoImport.{unidoc, unidocProjectFilter}
import sbtunidoc.JavaUnidocPlugin.autoImport.JavaUnidoc
import sbtunidoc.ScalaUnidocPlugin.autoImport.ScalaUnidoc

object Settings {
  def mergeSiteWith(p: Project): Setting[Task[Seq[(File, String)]]] =
    (mappings in makeSite) := {
      val cswVersion   = version.value
      val siteMappings = (mappings in makeSite).value ++ (mappings in makeSite in p).value

      val siteMappingsWithoutVersion = siteMappings.map { case (file, output) => (file, s"/$docsParentDir/" + output) }
      val siteMappingsWithVersion    = siteMappings.map { case (file, output) => (file, s"/$docsParentDir/" + cswVersion + output) }

      // keep documentation for SNAPSHOT versions in SNAPSHOT directory. (Don't copy SNAPSHOT docs to top level)
      // If not SNAPSHOT version, then copy latest version of documentation to top level as well as inside corresponding version directory
      if (cswVersion.endsWith("-SNAPSHOT")) siteMappingsWithVersion
      else siteMappingsWithoutVersion ++ siteMappingsWithVersion
    }

  def docExclusions(projects: Seq[ProjectReference]): Seq[Setting[_]] =
    projects.map(p => sources in (Compile, doc) in p := Seq.empty) ++ Seq(
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*),
      unidocProjectFilter in (JavaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*)
    )

  def addAliases: Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      "test; multi-jvm:test"
    ) ++
    addCommandAlias(
      "buildAll",
      ";set every enableFatalWarnings := true; scalafmtCheck; scalastyle; clean; makeSite; test:compile; multi-jvm:compile; set every enableFatalWarnings := false"
    )
  }

  def multiJvmTestTask(multiJvmProjects: Seq[ProjectReference]): Seq[Setting[_]] = {
    val tasks: Seq[Def.Initialize[Task[Unit]]] = multiJvmProjects.map(p => p / MultiJvm / test)

    Seq(
      MultiJvm / test / aggregate := false,
      MultiJvm / test := Def.sequential(tasks.init, tasks.last).value
    )
  }
}
