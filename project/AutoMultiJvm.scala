import sbt.Keys._
import sbt._

object AutoMultiJvm extends AutoPlugin {
  import com.typesafe.sbt.SbtMultiJvm
  import SbtMultiJvm.MultiJvmKeys._

  override def projectSettings: Seq[Def.Setting[_]] = SbtMultiJvm.multiJvmSettings ++ Seq(
    test := {
      (test in Test).value
      (test in MultiJvm).value
    },
    multiNodeHosts in MultiJvm := sys.env.getOrElse("multiNodeHosts", "localhost").split(",").toSeq
  )

  override def projectConfigurations: Seq[Configuration] = List(MultiJvm)
}
