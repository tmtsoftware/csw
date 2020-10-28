package csw.params.core.formats

import csw.params.core.generics.KeyType.{IntKey, StringKey}
import csw.params.core.generics.Parameter
import io.bullet.borer.{Cbor, Decoder, Encoder, Json}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FlatParamSetTest extends AnyFunSuite with Matchers {

  test("flat encoding of paramSet is possible for DMS specific needs to create FITS headers") {
    import ParamCodecs.FlatParamCodecs._

    val json =
      """
        |[
        | {"keyType":"IntKey","keyName":"IntKey","values":[70,80],"units":"NoUnits"},
        | {"keyType":"StringKey","keyName":"StringKey","values":["Str1","Str2"],"units":"NoUnits"}
        |]
        |""".stripMargin

    val p6  = IntKey.make("IntKey").set(70, 80)
    val p26 = StringKey.make("StringKey").set("Str1", "Str2")

    val paramSet: Set[Parameter[_]] = Set(p6, p26)

    val string = Json.encode(paramSet).toUtf8String
    val bytes  = Cbor.encode(paramSet).toByteArray

    val jsonDecoding  = Json.decode(json.getBytes()).to[Set[Parameter[_]]].value
    val jsonRoundTrip = Json.decode(string.getBytes()).to[Set[Parameter[_]]].value
    val cborRoundTrip = Cbor.decode(bytes).to[Set[Parameter[_]]].value

    jsonDecoding shouldBe paramSet
    jsonRoundTrip shouldBe paramSet
    cborRoundTrip shouldBe paramSet
  }

}
