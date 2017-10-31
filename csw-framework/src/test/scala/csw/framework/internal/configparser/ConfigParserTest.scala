package csw.framework.internal.configparser

import com.typesafe.config.ConfigFactory
import csw.messages.framework.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.framework.models.ContainerInfo
import csw.messages.framework.ComponentInfo
import csw.messages.location.Connection
import csw.messages.location.ComponentType.{Assembly, HCD}
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

// DEOPSCSW-167: Creation and Deployment of Standalone Components
// DEOPSCSW-170: Starting component using a file format
// DEOPSCSW-283: Parsing HOCON conf file
class ConfigParserTest extends FunSuite with Matchers {

  private val assemblyInfo = ComponentInfo(
    "Assembly-1",
    Assembly,
    "tcs.mobie.blue.filter",
    "csw.pkgDemo.assembly1.Assembly1",
    DoNotRegister,
    Set(Connection.from("HCD2A-hcd-akka"), Connection.from("HCD2C-hcd-akka")),
    5.seconds
  )
  private val hcd2AInfo =
    ComponentInfo(
      "HCD-2A",
      HCD,
      "tcs.mobie.blue.filter",
      "csw.pkgDemo.hcd2.Hcd2",
      RegisterOnly,
      Set.empty
    )
  private val hcd2BInfo =
    ComponentInfo(
      "HCD-2B",
      HCD,
      "tcs.mobie.blue.disperser",
      "csw.pkgDemo.hcd2.Hcd2",
      DoNotRegister,
      Set.empty
    )

  private val containerInfo = ContainerInfo("Container-1", Set(assemblyInfo, hcd2AInfo, hcd2BInfo))

  test("should able to parse container config") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/SampleContainer.conf")
    ConfigParser.parseContainer(config) shouldEqual containerInfo
  }

  test("should able to parse standalone assembly config") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/standalone/SampleStandalone.conf")
    ConfigParser.parseStandalone(config) shouldEqual assemblyInfo
  }

  test("should able to throw error when 'name' is missing") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/container/missing_componentname.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'components' is missing") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/container/missing_components.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'components' is not a config object") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/container/invalid_components.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'behaviorFactoryClassName' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_classname.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'prefix' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_prefix.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'locationServiceUsage' is missing for assembly") {
    val config =
      ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_location_service_usage.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'connections' is not an array for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/invalid_connections.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when connection ingredients has typos for 'connections' in assembly") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/connection_entry_typo.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'componentType' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/assembly/missing_componenttype.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'behaviorFactoryClassName' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_classname.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'prefix' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_prefix.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'locationServiceUsage' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_location_service_usage.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'componentType' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/hcd/missing_componenttype.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'name' is missing for standalone mode") {
    val config = ConfigFactory.parseResources(getClass, "/parsing_test_conf/standalone/invalid_standalone.conf")

    intercept[java.lang.RuntimeException] {
      ConfigParser.parseStandalone(config)
    }
  }
}
