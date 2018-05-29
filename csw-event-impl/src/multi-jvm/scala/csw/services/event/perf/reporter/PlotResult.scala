package csw.services.event.perf.reporter

final case class PlotResult(values: Vector[(String, Number)] = Vector.empty) {

  def add(key: String, value: Number): PlotResult =
    copy(values = values :+ (key → value))

  def addAll(p: PlotResult): PlotResult =
    copy(values ++ p.values)

  val (labels, results) = values.unzip

  def csvLabels: String = labels.mkString("\"", "\",\"", "\"")

  def csvValues: String = values.mkString("\"", "\",\"", "\"")

  // this can be split to two lines with bash: cut -d':' -f2,3 | tr ':' $'\n'
  def csv(name: String): String = s"PLOT_$name:$csvLabels:$csvValues"

  def labelsStr: String  = labels.mkString(",")
  def resultsStr: String = results.map(x ⇒ f"${x.doubleValue()}%.2f").mkString("        ")
}

final case class ThroughputPlots(
    throughput: PlotResult = PlotResult(),
    dropped: PlotResult = PlotResult(),
    outOfOrder: PlotResult = PlotResult()
) {
  def printTable(): Unit = {
    println("=================================== Throughput =================================================")
    println("\t\t\t Throughput(msgs/s) \t\tTotal Dropped \t\t Out Of Order \t")
    println("================================================================================================")
    throughput.labels.zipWithIndex.foreach {
      case (label, index) ⇒
        println(s"$label: ${if (label.length < 7) "\t\t\t" else "\t\t"} ${throughput.results(index).intValue()} \t\t\t ${dropped
          .results(index)
          .intValue()} \t\t\t ${outOfOrder.results(index).intValue()} \t")
    }
    println("\n")
  }
}

final case class LatencyPlots(
    plot50: PlotResult = PlotResult(),
    plot90: PlotResult = PlotResult(),
    plot99: PlotResult = PlotResult(),
    avg: PlotResult = PlotResult()
) {
  def printTable(): Unit = {
    println("================================== Latency in µs ===========================================================")
    println("\t\t\t 50%tile \t\t 90%tile \t\t 99%tile \t\t Average \t")
    println("============================================================================================================")

    plot50.labels.zipWithIndex.foreach {
      case (label, index) ⇒
        println(s"$label: ${if (label.length < 6) "\t\t" else "\t"} ${plot50.results(index)} \t\t ${plot90
          .results(index)} \t\t ${plot99.results(index)} \t\t ${avg.results(index)} \t")
    }
    println("\n")
  }
}
