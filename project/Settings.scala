import sbt._
import Keys._
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport.{builtinParadoxTheme, paradoxProperties, paradoxTheme}

object Settings {

  lazy val defaultParadoxSettings: Seq[Setting[_]] = Seq(
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    paradoxProperties ++= Map(
      "version" -> version.value,
      "scala.binaryVersion" -> scalaBinaryVersion.value,
      "extref.akka-docs.base_url" -> s"http://doc.akka.io/docs/akka/${Akka.Version}/%s.html",
      "extref.java-api.base_url" -> "https://docs.oracle.com/javase/8/docs/api/index.html?%s.html",
      "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/akka/${Akka.Version}",
      "scaladoc.csw.base_url" -> s"http://tmtsoftware.github.io/csw-prod/api/${version.value}"
    ),
    sourceDirectory := baseDirectory.value / "src" / "main"
  )

}
