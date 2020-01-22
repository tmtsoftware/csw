package csw.aas.core

import csw.aas.core.token.claims.{Audience, TokenSubsystems}
import csw.prefix.models.Subsystem.{CSW, ESW}
import io.bullet.borer.Json
import org.scalatest.{Matchers, WordSpecLike}

// DEOPSCSW-558: SPIKE: Token containing user info and roles
// DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class AuthCodecsTest extends WordSpecLike with AuthCodecs with Matchers {
  "AudienceCodec" must {
    //DECODING
    "decode empty null to an empty sequence" in {
      val str      = "null"
      val audience = Json.decode(str.getBytes).to[Audience].value
      audience should ===(Audience(Seq.empty))
    }
    "decode empty string to and empty sequence" in {
      val str      = "\"\""
      val audience = Json.decode(str.getBytes).to[Audience].value
      audience should ===(Audience(Seq.empty))
    }
    "decode empty array to and empty sequence" in {
      val str      = "[]"
      val audience = Json.decode(str.getBytes).to[Audience].value
      audience should ===(Audience(Seq.empty))
    }
    "decode a string to single valued sequence" in {
      val str      = "\"ABC\""
      val audience = Json.decode(str.getBytes).to[Audience].value
      audience should ===(Audience("ABC"))
    }
    "decode array of strings to sequence of strings" in {
      val str      = "[\"A\",\"B\"]"
      val audience = Json.decode(str.getBytes).to[Audience].value
      audience should ===(Audience(Seq("A", "B")))
    }

    //ENCODING
    "encode a single valued sequence to a single string" in {
      val aud = Audience("ABC")
      val str = Json.encode(aud).toUtf8String
      str should ===("\"ABC\"")
    }
    "encode an sequence with multiple values to an array" in {
      val aud = Audience(Seq("ABC", "XYZ"))
      val str = Json.encode(aud).toUtf8String
      str should ===("[\"ABC\",\"XYZ\"]")
    }
    "encode an empty sequence to null" in {
      val aud = Audience()
      val str = Json.encode(aud).toUtf8String
      str should ===("null")
    }
  }
  "TokenSubsystemsCodec" must {
    //DECODING
    "decode empty null to an empty sequence" in {
      val str      = "null"
      val audience = Json.decode(str.getBytes).to[TokenSubsystems].value
      audience should ===(TokenSubsystems.empty)
    }
    "decode empty string to and empty sequence" in {
      val str      = "\"\""
      val audience = Json.decode(str.getBytes).to[TokenSubsystems].value
      audience should ===(TokenSubsystems.empty)
    }
    "decode a upper case string to a single valued set of subsystems" in {
      val str      = "\"CSW\""
      val audience = Json.decode(str.getBytes).to[TokenSubsystems].value
      audience should ===(TokenSubsystems(Set(CSW)))
    }
    "decode a lower case string to a single valued set of subsystems" in {
      val str      = "\"esw\""
      val audience = Json.decode(str.getBytes).to[TokenSubsystems].value
      audience should ===(TokenSubsystems(Set(ESW)))
    }
    "decode comma separated string to set of subsystems" in {
      val str      = "\"csw, ESW\""
      val audience = Json.decode(str.getBytes).to[TokenSubsystems].value
      audience should ===(TokenSubsystems(Set(ESW, CSW)))
    }
    "must ignore any invalid subsystems" in {
      val str      = "\"invalid\""
      val audience = Json.decode(str.getBytes).to[TokenSubsystems].value
      audience should ===(TokenSubsystems())
    }

    //ENCODING
    "encode a single valued sequence to a single string" in {
      val aud = TokenSubsystems(Set(CSW))
      val str = Json.encode(aud).toUtf8String
      str should ===("\"CSW\"")
    }
    "encode a multi valued sequence to a comma separated string" in {
      val aud = TokenSubsystems(Set(CSW, ESW))
      val str = Json.encode(aud).toUtf8String
      str should ===("\"CSW, ESW\"")
    }
    "encode an sequence to null" in {
      val aud = TokenSubsystems.empty
      val str = Json.encode(aud).toUtf8String
      str should ===("null")
    }
  }
}
