package csw.services.location

import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import csw.services.location.helpers.LSMultiNodeSpec

class DemoTestMultiJvmNode1 extends DemoTest
class DemoTestMultiJvmNode2 extends DemoTest

class DemoTest extends LSMultiNodeSpec {
  override def initialParticipants: Int = roles.size

  test("blah blah") {
    awaitAssert {
      DistributedData(system).replicator ! GetReplicaCount
      expectMsg(ReplicaCount(roles.size))
    }
    enterBarrier("after-1")
  }
}
