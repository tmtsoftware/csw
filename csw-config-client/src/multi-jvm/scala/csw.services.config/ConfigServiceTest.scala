package csw.services.config

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import com.typesafe.config.ConfigFactory
import csw.services.config.api.models.ConfigData
import csw.services.config.client.internal.ActorRuntime
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.commons.TestFileUtils
import csw.services.config.server.{ServerWiring, Settings}
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.services.location.scaladsl.LocationServiceFactory

class ConfigServiceTestMultiJvmNode1 extends ConfigServiceTest(0)
class ConfigServiceTestMultiJvmNode2 extends ConfigServiceTest(0)

class ConfigServiceTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) {

  import config._

  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()
  }

  test("ensure that the cluster is up") {
    enterBarrier("nodes-joined")
    awaitAssert {
      DistributedData(system).replicator ! GetReplicaCount
      expectMsg(ReplicaCount(roles.size))
    }
    enterBarrier("cluster-formed")
  }

  test("should start config service server on one node and client should able to create and get files from other node") {

    runOn(seed) {
      val serverWiring = ServerWiring.make(locationService)
      serverWiring.svnRepo.initSvnRepo()
      serverWiring.httpService.lazyBinding.await
      enterBarrier("server-started")
    }

    runOn(member) {
      enterBarrier("server-started")
      val actorRuntime = new ActorRuntime(ActorSystem())
      import actorRuntime._
      val configService = ConfigClientFactory.make(actorSystem, locationService)

      val configValue: String =
        """
          |axisName1 = tromboneAxis1
          |axisName2 = tromboneAxis2
          |axisName3 = tromboneAxis3
          |""".stripMargin

      val file = Paths.get("test.conf")
      configService.create(file, ConfigData.fromString(configValue), oversize = false, "commit test file").await
      val actualConfigValue = configService.get(file).await.get.toStringF.await
      actualConfigValue shouldBe configValue
    }
  }
}
