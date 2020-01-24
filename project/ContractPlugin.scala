import com.typesafe.sbt.site.SitePlugin
import com.typesafe.sbt.site.SitePlugin.autoImport._
import sbt.Keys._
import sbt._

import scala.language.postfixOps

object ContractPlugin extends AutoPlugin {

  override def requires: Plugins = SitePlugin

  object autoImport {
    val generateDocs    = taskKey[Seq[(File, String)]]("generate contracts")
    val generateDocsDir = settingKey[String]("top folder for the generated contracts")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    generateDocsDir := "output",
    siteSubdirName := "/" + generateDocsDir.value,
    addMappingsToSiteDir(generateDocs, siteSubdirName)
  )

  def generate(generatorProject: Project) = Def.taskDyn {
    val outputDir = s" ${target.value}/${generateDocsDir.value}"
    Def.task {
      (generatorProject / Compile / run).toTask(outputDir).value
      Path.contentOf(target.value / generateDocsDir.value)
    }
  }
}
