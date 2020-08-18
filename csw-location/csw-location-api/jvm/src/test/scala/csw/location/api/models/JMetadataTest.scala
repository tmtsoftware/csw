package csw.location.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import org.scalactic.TripleEqualsSupport
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class JMetadataTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with TripleEqualsSupport {

  test("should be able to get value from metadata for given key with Java APIs | CSW-108") {
    val customKey = "customKey1"
    val value     = "value1"
    val metadata  = Metadata().add(customKey, value)

    metadata.jGet(customKey).get() should ===(value)

    metadata.jGet("invalidKey").isEmpty should ===(true)
    metadata.jGetAgentPrefix.isEmpty should ===(true)
    metadata.jGetPid.isEmpty should ===(true)
  }

  test("should be able to get pid from metadata| CSW-108") {
    val pid      = 1234
    val metadata = Metadata().withPid(pid)

    metadata.jGetPid.get() should ===(pid)
  }

  test("should be able to get agent prefix from metadata| CSW-108") {
    val agentPrefix = Prefix(ESW, "agent1")

    val metadata = Metadata().withAgentPrefix(agentPrefix)

    metadata.jGetAgentPrefix.get() should ===(agentPrefix)
  }
}
