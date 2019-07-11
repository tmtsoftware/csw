import org.scalajs.sbtplugin.ScalaJSPlugin
import sbt.{Def, _}
import sbt.Keys._

object CrossProjectSettings {
  // Will be true when SBT is run by IntelliJ for project import (NOT sbt shell)
  // For this to work, you MUST NOT use the "Use sbt shell for build and import" feature in IntelliJ
  val forIdeaImport: Boolean = System.getProperty("idea.managed", "false").toBoolean &&
    System.getProperty("idea.runid") == null

  def mkSourceDirs(base: File, scalaBinary: String, conf: String): Seq[File] = Seq(
    base / "src" / conf / "scala",
    base / "src" / conf / s"scala-$scalaBinary",
    base / "src" / conf / "java"
  )

  def sourceDirsSettings(baseMapper: File => File): Seq[Setting[Seq[File]]] = Seq(
    unmanagedSourceDirectories in Compile ++=
      mkSourceDirs(baseMapper(baseDirectory.value), scalaBinaryVersion.value, "main"),
    unmanagedSourceDirectories in Test ++=
      mkSourceDirs(baseMapper(baseDirectory.value), scalaBinaryVersion.value, "test")
  )

  def jsProjectSetup(jvmProject: Project)(jsProject: Project): Project = {
    jsProject
      .in(jvmProject.base / "js")
      .enablePlugins(ScalaJSPlugin)
      .configure(p => if (forIdeaImport) p.dependsOn(jvmProject) else p)
      .settings(
        sourceDirsSettings(_.getParentFile),
        name := (name in jvmProject).value,
        fork := false
      )
  }
}
