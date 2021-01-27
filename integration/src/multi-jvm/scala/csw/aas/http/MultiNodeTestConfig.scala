package csw.aas.http
import akka.remote.testconductor.RoleName
import csw.location.helpers.NMembersAndSeed

class MultiNodeTestConfig extends NMembersAndSeed(2) {

  val keycloak: RoleName = seed
  val (exampleServer, testClient) = members match {
    case Vector(exampleServer, testClient) => (exampleServer, testClient)
    case x                                 => throw new MatchError(x)
  }
}
