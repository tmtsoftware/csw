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

      val siteMappingsWithoutVersion = siteMappings.map { case (file, output) => (file, "/csw/" + output) }
      val siteMappingsWithVersion    = siteMappings.map { case (file, output) => (file, "/csw/" + cswVersion + output) }

      // keep documentation for SNAPSHOT versions in SNAPSHOT directory. (Don't copy SNAPSHOT docs to top level)
      // If not SNAPSHOT version, then copy latest version of documentation to top level as well as inside corresponding version directory
      if (cswVersion.endsWith("-SNAPSHOT")) siteMappingsWithVersion
      else siteMappingsWithoutVersion ++ siteMappingsWithVersion
    }

  def docExclusions(projects: Seq[ProjectReference]): Seq[Setting[_]] =
    projects.map(p ⇒ sources in (Compile, doc) in p := Seq.empty) ++ Seq(
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*),
      unidocProjectFilter in (JavaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*)
    )
}
