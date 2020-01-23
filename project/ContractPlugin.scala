import sbt.Keys._
import sbt._

import scala.language.postfixOps

object ContractPlugin extends AutoPlugin {
  object autoImport {
    val generateDocs    = taskKey[Seq[(File, String)]]("generate contracts")
    val generateDocsDir = settingKey[String]("top folder for the generated contracts")
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    generateDocsDir := "output",
    generateDocs := generate().value
  )

  private def generate() = Def.taskDyn {
    val outputDir = s" ${target.value}/${generateDocsDir.value}"
    Def.task {
      (Compile / run).toTask(outputDir).value
      Path.contentOf(target.value / generateDocsDir.value)
    }
  }
}
