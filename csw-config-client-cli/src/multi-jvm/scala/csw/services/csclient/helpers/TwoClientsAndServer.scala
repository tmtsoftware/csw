package csw.services.csclient.helpers

import csw.services.location.helpers.NMembersAndSeed
import akka.remote.testconductor.RoleName

class TwoClientsAndServer extends NMembersAndSeed(2) {
  val server: RoleName         = seed
  val Vector(client1, client2) = members
}
