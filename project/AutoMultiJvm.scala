import sbt.Keys.{moduleName, packageBin}
import sbt.{Def, _}

object AutoMultiJvm extends AutoPlugin {
  import com.typesafe.sbt.SbtMultiJvm
  import SbtMultiJvm.MultiJvmKeys._
  import sbtassembly.AssemblyKeys._
  import sbtassembly.MergeStrategy

  lazy val reverseConcat: MergeStrategy = new MergeStrategy {
    override def name: String = "reverseConcat"

    override def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] =
      MergeStrategy.concat(tempDir, path, files.reverse)
  }

  override def projectSettings: Seq[Setting[_]] =
    SbtMultiJvm.multiJvmSettings ++ Seq(
      multiNodeHosts in MultiJvm := multiNodeHostNames,
      assemblyMergeStrategy in assembly in MultiJvm := {
        case "application.conf"                     => reverseConcat
        case x if x.contains("versions.properties") => MergeStrategy.discard
        case x if x.contains("mailcap.default")     => MergeStrategy.last
        case x if x.contains("mimetypes.default")   => MergeStrategy.last
        case x if x.contains("schema")              => MergeStrategy.last
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly in MultiJvm).value
          oldStrategy(x)
      }
    )

  override def projectConfigurations: Seq[Configuration] = List(MultiJvm)

  private def multiNodeHostNames =
    sys.env.get("multiNodeHosts") match {
      case Some(str) => str.split(",").toSeq
      case None      => Seq.empty
    }

  lazy val multiJvmArtifact = Def.setting(Artifact(moduleName.value, "multi-jvm"))
}
