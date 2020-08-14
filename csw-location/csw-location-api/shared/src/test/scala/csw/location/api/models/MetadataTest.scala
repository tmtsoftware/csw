package csw.location.api.models

import org.scalactic.TripleEqualsSupport
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class MetadataTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with TripleEqualsSupport {

  test("should be able to create metadata with agent prefix and PID | DEOPSCSW-108") {
    val agentPrefix = "ESW.agent1"
    val PID         = "1234"

    val metadata = Metadata().withAgent(agentPrefix).withPID(PID)

    metadata.value should ===(Map("agentPrefix" -> agentPrefix, "PID" -> PID))
  }

  test("should be able to create metadata with given key | DEOPSCSW-108") {
    val value1 = "1234"
    val value2 = "abc"

    val metadata = Metadata().add("customKey1", value1).add("customKey2", value2)

    metadata.value should ===(Map("customKey1" -> value1, "customKey2" -> value2))
  }

  test("should be able to get value from metadata for given key | DEOPSCSW-108") {
    val customKey = "customKey1"
    val value1    = "value1"
    val metadata  = Metadata().add(customKey, value1)

    metadata.get(customKey).get should ===(value1)

    metadata.get("invalidKey") should ===(None)
    metadata.getAgentPrefix should ===(None)
    metadata.getPID should ===(None)
  }

  test("should be able to get value from metadata for agentPrefix key | DEOPSCSW-108") {
    val value1   = "ESW.agent"
    val metadata = Metadata().withAgent(value1)

    metadata.getAgentPrefix.get should ===(value1)
  }

  test("should be able to get value from metadata for PID key | DEOPSCSW-108") {
    val value1   = "1234"
    val metadata = Metadata().withPID(value1)

    metadata.getPID.get should ===(value1)
  }

  test("should return None from metadata for invalid keys | DEOPSCSW-108") {
    val customKey = "customKey1"
    val value1    = "value1"
    val metadata  = Metadata().add(customKey, value1)

    metadata.get("invalidKey") should ===(None)
    metadata.getAgentPrefix should ===(None)
    metadata.getPID should ===(None)
  }
}
