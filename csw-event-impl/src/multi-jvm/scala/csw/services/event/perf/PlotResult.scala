package csw.services.event.perf

final case class PlotResult(values: Vector[(String, Number)] = Vector.empty) {

  def add(key: String, value: Number): PlotResult =
    copy(values = values :+ (key → value))

  def addAll(p: PlotResult): PlotResult =
    copy(values ++ p.values)

  private val (labels, results) = values.unzip

  def csvLabels: String = labels.mkString("\"", "\",\"", "\"")

  def csvValues: String = values.mkString("\"", "\",\"", "\"")

  // this can be split to two lines with bash: cut -d':' -f2,3 | tr ':' $'\n'
  def csv(name: String): String = s"PLOT_${name}:${csvLabels}:${csvValues}"

  def labelsStr: String  = labels.mkString("    ")
  def resultsStr: String = results.map(x ⇒ f"${x.doubleValue()}%.2f").mkString("        ")

}

final case class LatencyPlots(
    plot50: PlotResult = PlotResult(),
    plot90: PlotResult = PlotResult(),
    plot99: PlotResult = PlotResult()
) {
  def printTable(name: String) = {
    println("         " + plot50.labelsStr)
    println("50%tile: " + plot50.resultsStr)
    println("90%tile: " + plot90.resultsStr)
    println("99%tile: " + plot99.resultsStr)
  }
}
