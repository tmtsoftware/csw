package csw.location.helpers

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.actor.typed.scaladsl.adapter._
import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.CswCluster
import csw.location.server.internal.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T, mode: String = "cluster")
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = system.toTyped.asInstanceOf[ActorSystem[SpawnProtocol.Command]]
  protected val cswCluster: CswCluster                         = CswCluster.withSystem(typedSystem)
  lazy protected val locationService: LocationService = mode match {
    case "http"    => HttpLocationServiceFactory.makeLocalClient(typedSystem)
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
