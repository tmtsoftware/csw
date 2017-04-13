import sbt._

object Coverage extends AutoPlugin {
  import scoverage.ScoverageSbtPlugin
  import ScoverageSbtPlugin.autoImport._
  import org.scoverage.coveralls.CoverallsPlugin

  override def requires: Plugins = ScoverageSbtPlugin && CoverallsPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    coverageEnabled := true,
    coverageMinimum := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageOutputCobertura := true,
    coverageOutputXML := true
  )

}
