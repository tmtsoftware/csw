import sbt.Keys._
import sbt._

object AutoMultiJvm extends AutoPlugin {
  import com.typesafe.sbt.SbtMultiJvm
  import SbtMultiJvm.MultiJvmKeys

  override def projectSettings: Seq[Def.Setting[_]] = SbtMultiJvm.multiJvmSettings ++ Seq(
    test := {
      (test in Test).value
      (test in MultiJvmKeys.MultiJvm).value
    }
  )

  override def projectConfigurations: Seq[Configuration] = List(MultiJvmKeys.MultiJvm)
}
