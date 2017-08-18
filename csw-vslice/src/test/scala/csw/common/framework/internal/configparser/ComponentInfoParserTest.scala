package csw.common.framework.internal.configparser

import java.io.File

import com.typesafe.config.ConfigFactory
import csw.common.framework.models.ComponentInfo
import csw.common.framework.models.LocationServiceUsages.{RegisterAndTrackServices, RegisterOnly}
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.{ComponentType, Connection}
import org.scalatest.{FunSuite, Matchers}

class ComponentInfoParserTest extends FunSuite with Matchers {
  private val assemblyInfo = ComponentInfo(
    "Assembly-1",
    Assembly,
    "tcs.mobie.blue.filter",
    "csw.pkgDemo.assembly1.Assembly1",
    RegisterAndTrackServices,
    Some(Set(Connection.from("HCD2A-hcd-akka"), Connection.from("HCD2C-hcd-akka")))
  )
  private val hcd2AInfo = ComponentInfo("HCD-2A", HCD, "tcs.mobie.blue.filter", "csw.pkgDemo.hcd2.Hcd2", RegisterOnly)
  private val hcd2BInfo =
    ComponentInfo("HCD-2B", HCD, "tcs.mobie.blue.disperser", "csw.pkgDemo.hcd2.Hcd2", RegisterOnly)

  test("Should able to parse valid config file to ComponentInfo") {
    val path   = getClass.getResource("/conf/SampleContainer.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    val componentInfo: ComponentInfo = ComponentInfoParser.parseContainerConfig(config).get

    componentInfo.componentName shouldBe "Container-2"
    componentInfo.componentType shouldBe ComponentType.Container
    componentInfo.maybeComponentInfoes.get shouldEqual Set(assemblyInfo, hcd2AInfo, hcd2BInfo)
  }

  test("Should able to log error when 'componentName' is missing") {
    val path   = getClass.getResource("/conf/container/missing_componentname.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'components' is missing") {
    val path   = getClass.getResource("/conf/container/missing_components.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'components' is not a config object") {
    val path   = getClass.getResource("/conf/container/invalid_components.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'class', 'prefix' are missing for assembly") {
    val path   = getClass.getResource("/conf/assembly/missing_class_prefix.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'connections' are missing for assembly") {
    val path   = getClass.getResource("/conf/assembly/missing_connections.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'connections' is not an array for assembly") {
    val path   = getClass.getResource("/conf/assembly/invalid_connections.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when any one entry in 'connections' is having a typo for assembly") {
    val path   = getClass.getResource("/conf/assembly/connection_entry_typo.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'componentType' is missing for assembly") {
    val path   = getClass.getResource("/conf/assembly/missing_componenttype.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'class', 'prefix' are missing for hcd") {
    val path   = getClass.getResource("/conf/hcd/missing_class_prefix.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to log error when 'componentType' is missing for hcd") {
    val path   = getClass.getResource("/conf/hcd/missing_componenttype.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseContainerConfig(config) shouldEqual None
  }

  test("Should able to parse valid config file to ComponentInfo for standalone mode") {
    val path   = getClass.getResource("/conf/standalone/SampleStandalone.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseStandaloneConfig(config).get shouldEqual assemblyInfo
  }

  test("Should able to log error when 'components' contains more than one entry for standalone mode") {
    val path   = getClass.getResource("/conf/standalone/invalid_standalone.conf").getPath
    val config = ConfigFactory.parseFile(new File(path))

    ComponentInfoParser.parseStandaloneConfig(config) shouldEqual None
  }
}
