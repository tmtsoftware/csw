package csw.config.client.helpers

import akka.remote.testconductor.RoleName
import csw.location.helpers.NMembersAndSeed

class OneClientAndServer extends NMembersAndSeed(1) {
  val server: RoleName = seed
  val Vector(client)   = members
}
