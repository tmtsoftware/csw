import sbt.Keys.moduleName
import sbt.{Def, _}

object AutoMultiJvm extends AutoPlugin {
  import com.typesafe.sbt.SbtMultiJvm
  import SbtMultiJvm.MultiJvmKeys._
  import sbtassembly.AssemblyKeys._
  import sbtassembly.MergeStrategy

  private val reporterOptions: Seq[String] =
    // -C - to give fully qualified name of the custom reporter
    if (Common.storyReport) Seq("-C", "tmt.test.reporter.TestReporter")
    else Seq.empty

  lazy val reverseConcat: MergeStrategy = new MergeStrategy {
    override def name: String = "reverseConcat"

    override def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] =
      MergeStrategy.concat(tempDir, path, files.reverse)
  }

  override def projectSettings: Seq[Setting[_]] =
    SbtMultiJvm.multiJvmSettings ++ Seq(
      MultiJvm / multiNodeHosts := multiNodeHostNames,
      MultiJvm / scalatestOptions ++= reporterOptions,
      MultiJvm / assembly / assemblyMergeStrategy := {
        case "application.conf"                            => reverseConcat
        case x if x.contains("versions.properties")        => MergeStrategy.discard
        case x if x.contains("mailcap.default")            => MergeStrategy.last
        case x if x.contains("mimetypes.default")          => MergeStrategy.last
        case x if x.contains("schema")                     => MergeStrategy.last
        case x if x.contains("ScalaTestBundle.properties") => MergeStrategy.concat
        case x if x.contains("version.conf")               => MergeStrategy.first
        case x =>
          val oldStrategy = (MultiJvm / assembly / assemblyMergeStrategy).value
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
