import com.typesafe.sbt.site.util.SiteHelpers.addMappingsToSiteDir
import com.typesafe.sbt.site.SitePlugin
import sbt.Keys._
import sbt.{Def, _}

import scala.language.postfixOps

object ContractPlugin extends AutoPlugin {

  override def requires: Plugins = SitePlugin

  object autoImport {
    val generateDocs        = taskKey[Seq[(File, String)]]("generate contracts")
    val generateDocsDirName = "contracts"
    val generateDocsDirPath = settingKey[String]("path of the folder for the generated contracts")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    generateDocsDirPath := "/" + generateDocsDirName,
    addMappingsToSiteDir(generateDocs, generateDocsDirPath)
  )

  def generate(generatorProject: Project): Def.Initialize[Task[Seq[(File, String)]]] = Def.taskDyn {
    val outputDir   = s" ${target.value}/$generateDocsDirName"
    val resourceDir = "src/main/resources"
    Def.task {
      (generatorProject / Compile / run).toTask(s" $outputDir $resourceDir").value
      Path.contentOf(target.value / generateDocsDirName)
    }
  }
}
