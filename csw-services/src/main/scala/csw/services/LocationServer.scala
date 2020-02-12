package csw.services

import csw.location.server.{Main => LocationMain}

object LocationServer {
  def start(clusterPort: String): Unit =
    Service.start("Location Service", LocationMain.main(Array("--clusterPort", clusterPort)))
}
