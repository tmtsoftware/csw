import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import csw.services.config.api.commons.TestFileUtils
import csw.services.config.api.models.ConfigData
import csw.services.config.client.ClientWiring
import csw.services.config.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.services.config.server.{Main, ServerWiring, Settings}
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.BeforeAndAfterEach

class CustomServerWiring extends ServerWiring {
  override lazy val locationService: LocationService = LocationServiceFactory.withSettings(ClusterSettings().onPort(3552))
}

class CustomClientWiring extends ClientWiring {
  override lazy val locationService: LocationService = {
    val locationService1 = LocationServiceFactory.withSettings(ClusterSettings().joinLocal(3552))
    Thread.sleep(2000)
    locationService1
  }

}

class ConfigServiceTestMultiJvmNode1 extends ConfigServiceTest(0)
class ConfigServiceTestMultiJvmNode2 extends ConfigServiceTest(0)

class ConfigServiceTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) with BeforeAndAfterEach{

  import config._

  private val testFileUtils = new TestFileUtils(new Settings(ConfigFactory.load()))

  override def beforeAll(): Unit = {
    super.beforeAll()
    testFileUtils.deleteServerFiles()
  }

  override protected def afterEach(): Unit = {
    testFileUtils.deleteServerFiles()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    testFileUtils.deleteServerFiles()
  }

  test("should start config service server on one node and client should able to create and get files from other node") {

    runOn(server) {

      System.setProperty("clusterPort", "3552")
      Main.main(Array[String]())
      enterBarrier("server-started")

      enterBarrier("end")

      Main.shutdown()
      enterBarrier("shutdown")
    }

    runOn(client) {
      enterBarrier("server-started")
      val customClientWiring = new CustomClientWiring
      import customClientWiring._
      import actorRuntime._

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

      enterBarrier("end")

      locationService.shutdown()
      enterBarrier("shutdown")

    }

    enterBarrier("after-2")

  }

}
