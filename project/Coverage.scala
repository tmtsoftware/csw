import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._

  override def requires = ScoverageSbtPlugin

  override def projectSettings = Seq(
    coverageEnabled := true,
    coverageMinimum := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageOutputCobertura := false,
    coverageOutputXML := false
  )

}
