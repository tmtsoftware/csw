import java.io.File

import com.typesafe.sbt.site.SitePlugin.autoImport._
import sbt.Keys.mappings
import sbt.{Project, Setting, Task}

object Settings {
  def mergeSiteWith(p: Project): Setting[Task[Seq[(File, String)]]] = {
    (mappings in makeSite) := {
      (mappings in makeSite).value ++ (mappings in makeSite in p).value
    }
  }
}
