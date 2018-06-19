package csw.apps.clusterseed.cli

case class Options(clusterPort: Option[Int] = None, adminPort: Option[Int] = None, testMode: Boolean = false)
