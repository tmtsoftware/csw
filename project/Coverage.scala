import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._
  import org.scoverage.coveralls.CoverallsPlugin

  override def requires = ScoverageSbtPlugin && CoverallsPlugin

  override def projectSettings = Seq(
    coverageEnabled := true,
    coverageMinimum := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageOutputCobertura := true,
    coverageOutputXML := true
  )

}
