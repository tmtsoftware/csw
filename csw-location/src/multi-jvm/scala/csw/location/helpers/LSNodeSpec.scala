package csw.location.helpers

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.commons.CswCluster
import csw.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T, mode: String = "cluster")
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  protected val cswCluster: CswCluster     = CswCluster.withSystem(system)
  implicit val typedSystem: ActorSystem[_] = system.toTyped
  protected val locationService: LocationService = mode match {
    case "http"    => HttpLocationServiceFactory.makeLocalHttpClient(system, cswCluster.mat)
    case "cluster" => LocationServiceFactory.withCluster(cswCluster)
  }

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  test("ensure that location service is up for all the nodes") {
    locationService.list.await
    enterBarrier("cluster-formed")
  }

}
