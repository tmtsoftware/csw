package csw.services.csClient

import akka.Done
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.scaladsl.LocationServiceFactory

import scala.concurrent.Future

class CsClientApp(cswCluster: CswCluster) {
  val locationService= LocationServiceFactory.withCluster(cswCluster)

  def start(args: Array[String]): Any = ???
  def shutdown(): Future[Done] = ???
}

object CsClientApp extends App {
  new CsClientApp(CswCluster.withSettings(ClusterSettings())).start(args)
}