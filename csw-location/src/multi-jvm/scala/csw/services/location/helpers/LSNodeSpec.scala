package csw.services.location.helpers

import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.services.location.scaladsl.CswCluster
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T) extends MultiNodeSpec(config, config.makeSystem)
  with ImplicitSender
  with MultiNodeSpecCallbacks
  with FunSuiteLike
  with Matchers
  with BeforeAndAfterAll {

  protected val cswCluster: CswCluster = CswCluster.withSystem(system)

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()
}
