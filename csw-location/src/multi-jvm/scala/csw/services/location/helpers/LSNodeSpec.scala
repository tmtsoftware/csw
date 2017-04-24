package csw.services.location.helpers

import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.services.location.commons.CswCluster
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T)
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  protected val cswCluster: CswCluster           = CswCluster.withSystem(system)
  protected val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  test("ensure that location service is up for all the nodes") {
    locationService.list.await
    enterBarrier("cluster-formed")
  }

}
