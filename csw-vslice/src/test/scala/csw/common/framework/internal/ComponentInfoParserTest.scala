package csw.common.framework.internal

import java.io.File

import com.typesafe.config.ConfigFactory
import csw.common.framework.models.ComponentInfo.ContainerInfo
import csw.services.location.models.ComponentType
import org.scalatest.{FunSuite, Matchers}

class ComponentInfoParserTest extends FunSuite with Matchers {

  test("Should able to parse config file to ContainerInfo") {
    val path   = getClass.getResource("/TestContainer.conf").getPath
    val file   = new File(path)
    val config = ConfigFactory.parseFile(file)

    val componentInfo: ContainerInfo = ComponentInfoParser.parse(config)

    componentInfo.componentName shouldBe "Container-2"
    componentInfo.componentType shouldBe ComponentType.Container
    componentInfo.componentInfos.size shouldBe 3
  }
}
