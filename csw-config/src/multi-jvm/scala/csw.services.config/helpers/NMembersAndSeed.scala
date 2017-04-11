package csw.services.config.helpers

import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeConfig

class NMembersAndSeed(n: Int) extends MultiNodeConfig {

  val server: RoleName = addRole("server")

  val clients: Vector[RoleName] = (1 to n).toVector.map { x =>
    addRole(s"client-$x")
  }

  private def addRole(name: String): RoleName = {
    val node = role(name)
    nodeConfig _
    node
  }
}

class OneMemberAndSeed extends NMembersAndSeed(1) {
  val Vector(client) = clients
}

class TwoMembersAndSeed extends NMembersAndSeed(2) {
  val Vector(client1, client2) = clients
}
