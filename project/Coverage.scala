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
      coverageFailOnMinimum    := true,
      coverageHighlighting     := true,
      coverageOutputCobertura  := true,
      coverageOutputXML        := true
    )

}
