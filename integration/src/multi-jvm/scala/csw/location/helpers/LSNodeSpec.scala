package csw.location.helpers

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterSettings
import csw.location.server.internal.LocationServiceFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T, mode: String = "cluster")
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with AnyFunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = system.toTyped.asInstanceOf[ActorSystem[SpawnProtocol.Command]]
  protected lazy val clusterSettings: ClusterSettings = ClusterSettings.make(typedSystem)
  lazy protected val locationService: LocationService = mode match {
    case "http"    => HttpLocationServiceFactory.makeLocalClient(typedSystem)
    case "cluster" => LocationServiceFactory.make(clusterSettings)
  }

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  test(s"${this.suiteName}:${myself.name} ensure that location service is up for all the nodes") {
    locationService.list.await
    enterBarrier("cluster-formed")
  }

}
