package csw.params.core.formats

import csw.params.core.generics.KeyType.{IntKey, StringKey, StructKey}
import csw.params.core.generics.Parameter
import csw.params.core.models.Struct
import io.bullet.borer.{Cbor, Json}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FlatParamSetTest extends AnyFunSuite with Matchers {

  test("flat encoding without struct") {
    import FlatParamCodecs._

    val json =
      """
        |[
        | {"keyType":"IntKey","keyName":"IntKey","values":[70,80],"units":"NoUnits"},
        | {"keyType":"StringKey","keyName":"StringKey","values":["Str1","Str2"],"units":"NoUnits"}
        |]
        |""".stripMargin

    val intParam    = IntKey.make("IntKey").set(70, 80)
    val stringParam = StringKey.make("StringKey").set("Str1", "Str2")

    val paramSet: Set[Parameter[_]] = Set(intParam, stringParam)

    val string = Json.encode(paramSet).toUtf8String
    val bytes  = Cbor.encode(paramSet).toByteArray

    val jsonRoundTrip = Json.decode(string.getBytes()).to[Set[Parameter[_]]].value
    val cborRoundTrip = Cbor.decode(bytes).to[Set[Parameter[_]]].value
    val jsonDecoding  = Json.decode(json.getBytes()).to[Set[Parameter[_]]].value

    jsonRoundTrip shouldBe paramSet
    cborRoundTrip shouldBe paramSet
    jsonDecoding shouldBe paramSet
  }

  test("flat encoding should work with recursion created by Struct") {
    import FlatParamCodecs._

    val json =
      """
        |[{"keyType":"StructKey","keyName":"StructKey","values":[{"paramSet":[{"keyType":"IntKey","keyName":"IntKey","values":[70,80],"units":"NoUnits"}]}],"units":"NoUnits"}]
        |""".stripMargin

    val intParam                    = IntKey.make("IntKey").set(70, 80)
    val structParam                 = StructKey.make("StructKey").set(Struct(Set(intParam)))
    val paramSet: Set[Parameter[_]] = Set(structParam)

    val string = Json.encode(paramSet).toUtf8String
    val bytes  = Cbor.encode(paramSet).toByteArray

    val jsonRoundTrip = Json.decode(string.getBytes()).to[Set[Parameter[_]]].value
    val cborRoundTrip = Cbor.decode(bytes).to[Set[Parameter[_]]].value
    val jsonDecoding  = Json.decode(json.getBytes()).to[Set[Parameter[_]]].value

    jsonRoundTrip shouldBe paramSet
    cborRoundTrip shouldBe paramSet
    jsonDecoding shouldBe paramSet
  }

  test("flat encoding for DMS specific needs to create FITS headers") {
    import FlatParamCodecs._

    val json =
      """
        |[
        | {"keyType":"IntKey","keyName":"IntKey","values":[70,80],"units":"NoUnits"},
        | {"keyType":"StringKey","keyName":"StringKey","values":["Str1","Str2"],"units":"NoUnits"},
        | {"keyType":"StructKey","keyName":"StructKey","values":[{"paramSet":[{"keyType":"IntKey","keyName":"IntKey","values":[70,80],"units":"NoUnits"},{"keyType":"StringKey","keyName":"StringKey","values":["Str1","Str2"],"units":"NoUnits"}]}],"units":"NoUnits"}
        |]
        |""".stripMargin

    val intParam    = IntKey.make("IntKey").set(70, 80)
    val stringParam = StringKey.make("StringKey").set("Str1", "Str2")
    val structParam = StructKey.make("StructKey").set(Struct(Set(intParam, stringParam)))

    val paramSet: Set[Parameter[_]] = Set(intParam, stringParam, structParam)

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
