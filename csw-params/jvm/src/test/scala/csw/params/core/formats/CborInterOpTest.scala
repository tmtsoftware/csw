package csw.params.core.formats

import csw.params.core.formats.ParamCodecs._
import csw.params.core.generics.{Key, Parameter}
import csw.params.javadsl.JKeyType
import io.bullet.borer.Cbor
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CborInterOpTest extends AnyFunSuite with Matchers {

  test("should decode java bytes to scala bytes") {
    val jByteKey: Key[java.lang.Byte]    = JKeyType.ByteKey.make("bytes")
    val param: Parameter[java.lang.Byte] = jByteKey.setAll("abc".getBytes().map(x => x: java.lang.Byte))
    val bytes: Array[Byte]               = Cbor.encode(param).toByteArray

    val parsedParam = Cbor.decode(bytes).to[Parameter[Byte]].value
    parsedParam shouldEqual param
  }

  test("should decode java Integers to scala ints") {
    val jIntKey: Key[Integer]     = JKeyType.IntKey.make("ints")
    val param: Parameter[Integer] = jIntKey.setAll(Array(1, 2, 3).map(x => Integer.valueOf(x)))
    val bytes: Array[Byte]        = Cbor.encode(param).toByteArray

    val parsedParam = Cbor.decode(bytes).to[Parameter[Int]].value
    parsedParam shouldEqual param
  }
}
