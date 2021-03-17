package csw.location.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import org.scalactic.TripleEqualsSupport
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class MetadataTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with TripleEqualsSupport {

  test("should be able to create metadata with agent prefix and PID | CSW-108") {
    val agentPrefix = Prefix(ESW, "agent1")
    val PID         = 1234L

    val metadata = Metadata().withAgentPrefix(agentPrefix).withPid(PID)

    metadata.value should ===(Map("agentPrefix" -> agentPrefix.toString, "PID" -> PID.toString))
  }

  test("should be able to create metadata with given key | CSW-108") {
    val value1 = "1234"
    val value2 = "abc"

    val metadata = Metadata().add("customKey1", value1).add("customKey2", value2)

    metadata.value should ===(Map("customKey1" -> value1, "customKey2" -> value2))
  }

  test("should be able to get value from metadata for given key | CSW-108") {
    val customKey = "customKey1"
    val value     = "value1"
    val metadata  = Metadata().add(customKey, value)

    metadata.get(customKey).get should ===(value)

    metadata.get("invalidKey") should ===(None)
    metadata.getAgentPrefix should ===(None)
    metadata.getPid should ===(None)
  }

  test("should be able to get value from metadata for agentPrefix key | CSW-108") {
    val agentPrefix = Prefix(ESW, "agent1")
    val metadata    = Metadata().withAgentPrefix(agentPrefix)

    metadata.getAgentPrefix.get should ===(agentPrefix)
  }

  test("should be able to get value from metadata for PID key | CSW-108") {
    val pid      = 1234L
    val metadata = Metadata().withPid(pid)

    metadata.getPid.get should ===(pid)
  }

  test("should return None from metadata for invalid keys | CSW-108, CSW-133") {
    val customKey = "customKey1"
    val value     = "value1"
    val metadata  = Metadata().add(customKey, value)

    metadata.get("invalidKey") should ===(None)
    metadata.getAgentPrefix should ===(None)
    metadata.getPid should ===(None)
    metadata.getSequenceComponentPrefix should ===(None)
  }

  test("should be able to create metadata with sequence component prefix | CSW-133") {
    val sequenceComponentPrefix = Prefix(ESW, "seqcomp1")

    val metadata = Metadata().withSequenceComponentPrefix(sequenceComponentPrefix)

    metadata.value should ===(Map("sequenceComponentPrefix" -> sequenceComponentPrefix.toString))
    metadata.getSequenceComponentPrefix should ===(Some(sequenceComponentPrefix))
  }
}
