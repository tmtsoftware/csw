package csw.common.framework.internal.configparser

import com.typesafe.config.ConfigFactory
import csw.common.framework.models.LocationServiceUsage.{DoNotRegister, RegisterOnly}
import csw.common.framework.models.{ComponentInfo, ContainerInfo}
import csw.services.location.models.ComponentType.{Assembly, HCD}
import csw.services.location.models.Connection
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-283: Parsing HOCON conf file
// DEOPSCSW-170: Starting component using a file format
class ComponentInfoParserTest extends FunSuite with Matchers {

  private val assemblyInfo = ComponentInfo(
    "Assembly-1",
    Assembly,
    "tcs.mobie.blue.filter",
    "csw.pkgDemo.assembly1.Assembly1",
    DoNotRegister,
    Set(Connection.from("HCD2A-hcd-akka"), Connection.from("HCD2C-hcd-akka"))
  )
  private val hcd2AInfo =
    ComponentInfo("HCD-2A", HCD, "tcs.mobie.blue.filter", "csw.pkgDemo.hcd2.Hcd2", RegisterOnly, Set.empty)
  private val hcd2BInfo =
    ComponentInfo("HCD-2B", HCD, "tcs.mobie.blue.disperser", "csw.pkgDemo.hcd2.Hcd2", DoNotRegister, Set.empty)
  private val containerInfo = ContainerInfo("Container-1", RegisterOnly, Set(assemblyInfo, hcd2AInfo, hcd2BInfo))

  test("should able to parse container config") {
    val config = ConfigFactory.parseResources(getClass, "/conf/SampleContainer.conf")
    ComponentInfoParser.parseContainer(config) shouldEqual containerInfo
  }

  test("should able to parse standalone assembly config") {
    val config = ConfigFactory.parseResources(getClass, "/conf/standalone/SampleStandalone.conf")
    ComponentInfoParser.parseStandalone(config) shouldEqual assemblyInfo
  }

  test("should able to throw error when 'name' is missing") {
    val config = ConfigFactory.parseResources(getClass, "/conf/container/missing_componentname.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'locationServiceUsage' is missing") {
    val config = ConfigFactory.parseResources(getClass, "/conf/container/missing_location_seervice_usage.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'components' is missing") {
    val config = ConfigFactory.parseResources(getClass, "/conf/container/missing_components.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'components' is not a config object") {
    val config = ConfigFactory.parseResources(getClass, "/conf/container/invalid_components.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseContainer(config)
    }
  }

  test("should able to throw error when 'className' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/missing_classname.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'prefix' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/missing_prefix.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'locationServiceUsage' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/missing_location_service_usage.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'connections' are missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/missing_connections.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'connections' is not an array for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/invalid_connections.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'connectionType' is missing for 'connections' in assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/connection_entry_typo.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'componentType' is missing for assembly") {
    val config = ConfigFactory.parseResources(getClass, "/conf/assembly/missing_componenttype.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'className' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/conf/hcd/missing_classname.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseComponent(config)
    }
  }

  test("should able to throw error when 'prefix' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/conf/hcd/missing_prefix.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'locationServiceUsage' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/conf/hcd/missing_location_service_usage.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'componentType' is missing for hcd") {
    val config = ConfigFactory.parseResources(getClass, "/conf/hcd/missing_componenttype.conf")

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseStandalone(config)
    }
  }

  test("should able to throw error when 'components' contains more than one entry for standalone mode") {
    val path   = "/conf/standalone/invalid_standalone.conf"
    val config = ConfigFactory.parseResources(getClass, path)

    intercept[java.lang.RuntimeException] {
      ComponentInfoParser.parseStandalone(config)
    }
  }
}
