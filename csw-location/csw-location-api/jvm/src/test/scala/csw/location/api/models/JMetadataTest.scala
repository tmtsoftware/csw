package csw.location.api.models

import org.scalactic.TripleEqualsSupport
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class JMetadataTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with TripleEqualsSupport {

  test("should be able to get value from metadata for given key with Java APIs | DEOPSCSW-108") {
    val customKey = "customKey1"
    val value1    = "value1"
    val metadata  = Metadata().add(customKey, value1)

    metadata.jGet(customKey).get() should ===(value1)

    metadata.jGet("invalidKey").isEmpty should ===(true)
    metadata.jGetAgentPrefix.isEmpty should ===(true)
    metadata.jGetPID.isEmpty should ===(true)
  }
}
