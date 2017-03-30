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
    multiNodeHosts in MultiJvm := multiNodeHostNames
  )

  def multiNodeHostNames: Seq[String] = sys.env.get("multiNodeHosts") match {
    case Some(str) ⇒ str.split(",").toSeq
    case None      ⇒ Seq.empty
  }

  override def projectConfigurations: Seq[Configuration] = List(MultiJvm)
}
