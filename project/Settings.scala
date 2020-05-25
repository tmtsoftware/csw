import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys.MultiJvm
import sbt.Keys._
import sbt._

object Settings {

  def addAliases: Seq[Setting[_]] = {
    addCommandAlias(
      "testAll",
      "test; multi-jvm:test"
    ) ++
    addCommandAlias(
      "compileAll",
      ";set every enableFatalWarnings := true; scalafmtCheck; scalastyle; test:compile; multi-jvm:compile; set every enableFatalWarnings := false"
    ) ++
    addCommandAlias(
      "buildAll",
      ";set every enableFatalWarnings := true; scalafmtCheck; scalastyle; clean; makeSite; test:compile; multi-jvm:compile; set every enableFatalWarnings := false;"
    )
  }

  def multiJvmTestTask(multiJvmProjects: Seq[ProjectReference]): Seq[Setting[_]] = {
    val tasks: Seq[Def.Initialize[Task[Unit]]] = multiJvmProjects.map(p => p / MultiJvm / test)

    Seq(
      MultiJvm / test / aggregate := false,
      MultiJvm / test := Def.sequential(tasks).value
    )
  }

  // export CSW_JS_VERSION env variable which is compatible with csw
  // this represents version number of javascript docs maintained at https://github.com/tmtsoftware/csw-js
  def cswJsVersion: String =
    (sys.env ++ sys.props).get("CSW_JS_VERSION") match {
      case Some(v) => v
      case None    => "0.1.0-SNAPSHOT"
    }
}
