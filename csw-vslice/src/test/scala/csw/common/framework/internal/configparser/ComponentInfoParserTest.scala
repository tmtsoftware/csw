package csw.common.framework.internal.configparser

import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, Matchers}

class ComponentInfoParserTest extends FunSuite with Matchers {
  test("container") {
    val config = ConfigFactory.parseResources(getClass, "/conf/SampleContainer.conf")
    ComponentInfoParser.parse(config)
  }

  test("standalone") {
    val config = ConfigFactory.parseResources(getClass, "/conf/standalone/SampleStandalone.conf")
    ComponentInfoParser.parseStandalone(config)
  }
}
