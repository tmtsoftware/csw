package csw.services.config.client.helpers

import akka.remote.testconductor.RoleName
import csw.services.location.helpers.NMembersAndSeed

class OneClientAndServer extends NMembersAndSeed(1) {
  val server: RoleName = seed
  val Vector(client)   = members
}
