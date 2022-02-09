import sbt.Keys._
import sbt._

object NoPublish extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      publishArtifact := false,
      publish         := {},
      publishLocal    := {}
    )
}

object CswBuildInfo extends AutoPlugin {
  import sbtbuildinfo.BuildInfoPlugin
  import BuildInfoPlugin.autoImport._

  override def requires: Plugins = BuildInfoPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      buildInfoKeys := Seq[BuildInfoKey](name, version),
      // module name(e.g. csw-alarm-cli) gets converted into package name(e.g. csw.alarm.cli) for buildInfo, so it does not have
      // same package across all modules in the repo
      buildInfoPackage := name.value.replace('-', '.')
    )
}
