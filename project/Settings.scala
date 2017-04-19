import java.io.File

import sbt._
import Keys._

import com.typesafe.sbt.site.SitePlugin.autoImport._
import sbtunidoc.BaseUnidocPlugin.autoImport.{unidoc, unidocProjectFilter}
import sbtunidoc.ScalaUnidocPlugin.autoImport.ScalaUnidoc

object Settings {
  def mergeSiteWith(p: Project): Setting[Task[Seq[(File, String)]]] =
    (mappings in makeSite) := {
      (mappings in makeSite).value ++ (mappings in makeSite in p).value
    }

  def docExclusions(projects: Seq[ProjectReference]): Seq[Setting[_]] =
    projects.map(p ⇒ sources in (Compile, doc) in p := Seq.empty) ++ Seq(
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(projects: _*)
    )
}
