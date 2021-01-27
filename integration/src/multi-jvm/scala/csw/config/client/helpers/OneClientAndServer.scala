package csw.config.client.helpers

import akka.remote.testconductor.RoleName
import csw.location.helpers.NMembersAndSeed

class OneClientAndServer extends NMembersAndSeed(1) {
  val server: RoleName = seed
  val client: RoleName = members match {
    case Vector(client) => client
    case x              => throw new MatchError(x)
  }

}
