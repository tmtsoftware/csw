package csw.aas.http
import akka.remote.testconductor.RoleName
import csw.location.helpers.NMembersAndSeed

class MultiNodeTestConfig extends NMembersAndSeed(2) {

  val keycloak: RoleName                = seed
  val Vector(exampleServer, testClient) = members
}
