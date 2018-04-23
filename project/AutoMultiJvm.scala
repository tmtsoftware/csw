import sbt.Keys._
import sbt._

object AutoMultiJvm extends AutoPlugin {
  import com.typesafe.sbt.SbtMultiJvm
  import SbtMultiJvm.MultiJvmKeys._
  import sbtassembly.AssemblyKeys._
  import sbtassembly.MergeStrategy

  override def projectSettings: Seq[Setting[_]] = SbtMultiJvm.multiJvmSettings ++ Seq(
    test := {
      (test in Test).value
      (test in MultiJvm).value
    },
    multiNodeHosts in MultiJvm := multiNodeHostNames,
    assemblyMergeStrategy in assembly in MultiJvm := {
      case "application.conf"                     => MergeStrategy.concat
      case x if x.contains("versions.properties") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly in MultiJvm).value
        oldStrategy(x)
    }
  )

  def multiNodeHostNames: Seq[String] = sys.env.get("multiNodeHosts") match {
    case Some(str) ⇒ str.split(",").toSeq
    case None      ⇒ Seq.empty
  }

  override def projectConfigurations: Seq[Configuration] = List(MultiJvm)
}
