import sbt.Keys._
import sbt._

import scala.language.postfixOps
import sbt.{Def, _}

object ContractPlugin extends AutoPlugin {

  override def requires: Plugins = SitePlugin

  object autoImport {
    val generateDocs        = taskKey[Seq[(File, String)]]("generate contracts")
    val generateDocsDirName = "output"
    val generateDocsDirPath = settingKey[String]("path of the folder for the generated contracts")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    generateDocsDirPath := "/" + generateDocsDirName,
    addMappingsToSiteDir(generateDocs, generateDocsDirPath)
  )

  def generate(generatorProject: Project): Def.Initialize[Task[Seq[(File, String)]]] = Def.taskDyn {
    val outputDir = s" ${target.value}/$generateDocsDirName"
    Def.task {
      (generatorProject / Compile / run).toTask(outputDir).value
      Path.contentOf(target.value / generateDocsDirName)
    }
  }
}
