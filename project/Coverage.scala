import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._

  override def requires: Plugins = ScoverageSbtPlugin

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      coverageExcludedPackages := "csw.event.client.internal.kafka",
      coverageExcludedPackages := "csw.command.client.messages",
      coverageExcludedPackages := "csw.framework.deploy.containercmd",
      coverageExcludedPackages := "csw.framework.exceptions",
      coverageExcludedPackages := "example.*",
      coverageEnabled          := true,
      coverageMinimumStmtTotal := 80,
      // XXX TODO FIXME: Scala3 coverage results may be lower than scala2
      // See https://github.com/scala/scala3/issues/21877
      // coverageFailOnMinimum    := true,
      coverageFailOnMinimum    := false,
      coverageHighlighting     := true,
      coverageOutputCobertura  := true,
      coverageOutputXML        := true
    )

}
