package csw.framework.internal.configparser

import com.typesafe.config.ConfigFactory
import csw.command.client.models.framework.ComponentInfo
import csw.command.client.models.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ConfigFileLocation.{Local, Remote}
import csw.framework.models.ContainerMode.{Container, Standalone}
import csw.framework.models.{ContainerBootstrapInfo, ContainerInfo, HostBootstrapInfo}
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection
import csw.prefix.models.Prefix
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-170: Starting component using a file format
// DEOPSCSW-172: Starting a container from configuration file
// DEOPSCSW-283: Parsing HOCON conf file
// CSW-82: ComponentInfo should take prefix
// CSW-83: Alarm models should take prefix
class ConfigParserTest extends AnyFunSuite with Matchers {

  private val assemblyInfo = ComponentInfo(
    Prefix("tcs.assembly1"),
    Assembly,
    "csw.pkgDemo.assembly1.Assembly1",
    DoNotRegister,
    Set(Connection.from("TCS.HCD2A-hcd-akka"), Connection.from("tcs.HCD2C-hcd-akka")),
    5.seconds
  )
  private val hcd2AInfo = ComponentInfo(Prefix("TCS.HCD2A"), HCD, "csw.pkgDemo.hcd2.Hcd2", RegisterOnly, Set.empty)
  private val hcd2BInfo = ComponentInfo(Prefix("TCS.HCD2B"), HCD, "csw.pkgDemo.hcd2.Hcd2", DoNotRegister, Set.empty)

  private val containerInfo = ContainerInfo("Container1", Set(assemblyInfo, hcd2AInfo, hcd2BInfo))

  private val containerBootstrapInfo = ContainerBootstrapInfo(
    Container,
    "/csw-framework/src/resources/laser_container.conf",
    Local
  )

  private val standaloneBootstrapInfo = ContainerBootstrapInfo(
    Standalone,
    "standalone.conf",
    Remote
  )

  private val hostBootstrapInfo = HostBootstrapInfo(Set(standaloneBootstrapInfo, containerBootstrapInfo))

  // ################### Start : Container Parsing ###################
  test("should able to parse container config | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/SampleContainer.conf")
    ConfigParser.parseContainer(config) shouldEqual containerInfo
  }

  test("should able to throw error when 'name' is missing | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/container/missing_componentname.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'components' is missing | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/container/missing_components.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseContainer(config)
    }
  }

  test(
    "should able to throw error when 'components' is not a config object | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/container/invalid_components.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseContainer(config)
    }
  }
  // ################### End : Container Parsing #####################

  // ################### Start : Standalone Parsing ##################
  test("should able to parse standalone assembly config | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283") {
    val config               = ConfigFactory.parseResources(getClass, "/parsing_test_conf/standalone/SampleStandalone.conf")
    val expectedAssemblyInfo = ConfigParser.parseStandalone(config)
    expectedAssemblyInfo shouldEqual assemblyInfo
    expectedAssemblyInfo.getConnections.asScala should contain allElementsOf assemblyInfo.connections

  }

  test(
    "should able to throw error when 'behaviorFactoryClassName' is missing for assembly | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_classname.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'subsystem' is missing for assembly | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_prefix.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'locationServiceUsage' is missing for assembly | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config =
      ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_location_service_usage.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'connections' is not an array for assembly | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/invalid_connections.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when connection ingredients has typos for 'connections' in assembly | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/connection_entry_typo.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'componentType' is missing for assembly | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_componenttype.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'behaviorFactoryClassName' is missing for hcd | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_classname.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'subsystem' is missing for hcd | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_prefix.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'locationServiceUsage' is missing for hcd | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_location_service_usage.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'componentType' is missing for hcd | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_componenttype.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test(
    "should able to throw error when 'name' is missing for standalone mode | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/standalone/invalid_standalone.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }
  // ################### End : Standalone Parsing ##################

  // DEOPSCSW-173: Host Configuration in file format
  // DEOPSCSW-175: Starting multiple containers from command Line
  // ################### Start : Host Parsing ######################
  test("should able to parse host config | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283, DEOPSCSW-173, DEOPSCSW-175") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hostconfig/valid_hostconfig.conf")
    ConfigParser.parseHost(config) shouldEqual hostBootstrapInfo
  }

  test(
    "should able to throw error when provided mode is invalid in host config | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283, DEOPSCSW-173, DEOPSCSW-175"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hostconfig/invalid_mode.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseHost(config)
    }
  }

  test(
    "should able to throw error when provided location is invalid in host config | DEOPSCSW-167, DEOPSCSW-170, DEOPSCSW-172, DEOPSCSW-283, DEOPSCSW-173, DEOPSCSW-175"
  ) {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hostconfig/invalid_location.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseHost(config)
    }
  }
  // ################### End : Host Parsing ######################
}
