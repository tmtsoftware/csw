import com.typesafe.sbt.site.SitePlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtunidoc.BaseUnidocPlugin.autoImport.{unidoc, unidocProjectFilter}
import sbtunidoc.JavaUnidocPlugin.autoImport.JavaUnidoc
import sbtunidoc.ScalaUnidocPlugin.autoImport.ScalaUnidoc

object Settings {
  def mergeSiteWith(p: Project): Setting[Task[Seq[(File, String)]]] =
    (mappings in makeSite) := {
      val globalMappings = (mappings in makeSite).value
      val projectMapping = (mappings in makeSite in p).value

      // this is to copy latest version of documentation to top level as well as inside corresponding version directory at gh-pages branch
      globalMappings ++
      globalMappings.map { case (file, output) => (file, "/" + version.value + output) } ++
      projectMapping ++
      projectMapping.map { case (file, output) => (file, "/" + version.value + output) }
    }

  def docExclusions(projects: Seq[ProjectReference]): Seq[Setting[_]] =
    projects.map(p ⇒ sources in (Compile, doc) in p := Seq.empty) ++ Seq(
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*),
      unidocProjectFilter in (JavaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*)
    )
}
