package csw.services.messages

import java.io
import java.time.Instant

import csw.messages.ccs.commands.{Observe, Setup, Wait}
import csw.messages.params.generics.KeyType.ByteKey
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.params.models.{ObsId, Prefix, Units}
import org.scalatest.FunSpec

import scala.util.Try

class CommandsTest extends FunSpec {

  //#obsid
  val obsId: ObsId = ObsId("Obs001")
  //#obsid

  it("Should show usage of Setup command") {
    //#setup
    //keys
    val k1: Key[Int]    = KeyType.IntKey.make("encoder")
    val k2: Key[String] = KeyType.StringKey.make("stringThing")
    val k2bad: Key[Int] = KeyType.IntKey.make("stringThing")
    val k3: Key[Int]    = KeyType.IntKey.make("filter")
    val k4: Key[Float]  = KeyType.FloatKey.make("correction")

    //prefix
    val prefix: Prefix = Prefix("wfos.red.detector")

    //parameters
    val i1: Parameter[Int]    = k1.set(22)
    val i2: Parameter[String] = k2.set("A")

    //create setup, add sequentially using add
    val sc1: Setup = Setup(obsId, prefix).add(i1).add(i2)

    //access keys
    val k1Exists: Boolean = sc1.exists(k1) //true

    //access parameters
    val tryParam1: Try[Parameter[Int]] = Try(sc1(k1))    //success
    val tryk2Bad: Try[Parameter[Int]]  = Try(sc1(k2bad)) //failure

    //add more than one parameters, using madd
    val sc2: Setup     = sc1.madd(k3.set(1, 2, 3, 4).withUnits(Units.day), k4.set(1.0f, 2.0f))
    val paramSize: Int = sc2.size

    //add binary payload
    val byteKey1: Key[Byte] = ByteKey.make("byteKey1")
    val byteKey2: Key[Byte] = ByteKey.make("byteKey2")
    val bytes1: Array[Byte] = Array[Byte](10, 20)
    val bytes2: Array[Byte] = Array[Byte](30, 40)

    val b1: Parameter[Byte] = byteKey1.set(bytes1)
    val b2: Parameter[Byte] = byteKey2.set(bytes2)

    val sc3: Setup = Setup(obsId, prefix, Set(b1, b2))

    //remove a key
    val sc4: Setup = sc3.remove(b1)

    //list all keys
    val allKeys: Set[String] = sc4.paramSet.map(_.keyName)

    //#setup

    //validations
    assert(k1Exists)
    assert(tryParam1.isSuccess)
    assert(tryk2Bad.isFailure)
    assert(paramSize === 4)
    assert(sc3.size === 2)
    assert(sc4.size === 1)
    assert(allKeys.size === 1)
  }

  it("Should show usage of Observe command") {
    //#observe
    //keys
    val k1: Key[Boolean] = KeyType.BooleanKey.make("repeat")
    val k2: Key[Int]     = KeyType.IntKey.make("expTime")
    val k2bad: Key[Int]  = KeyType.IntKey.make("stringThing")
    val k3: Key[Int]     = KeyType.IntKey.make("filter")
    val k4: Key[Instant] = KeyType.TimestampKey.make("creation-time")

    //prefix
    val prefix: Prefix = Prefix("wfos.red.detector")

    //parameters
    val i1: Parameter[Boolean] = k1.set(true, false, true, false)
    val i2: Parameter[Int]     = k2.set(1, 2, 3, 4)

    //create observe, add sequentially using add
    val oc1: Observe = Observe(obsId, prefix).add(i1).add(i2)

    //access params using apply method
    val k1Param: Parameter[Boolean] = oc1(k1) //true
    val values: Array[Boolean]      = k1Param.values

    //access parameters
    val tryParam1: Try[Parameter[Boolean]] = Try(oc1(k1))    //success
    val tryk2Bad: Try[Parameter[Int]]      = Try(oc1(k2bad)) //failure

    //add more than one parameters, using madd
    val oc2: Observe   = oc1.madd(k3.set(1, 2, 3, 4).withUnits(Units.day), k4.set(Instant.now()))
    val paramSize: Int = oc2.size

    //update existing key with set
    val oc3: Observe = oc1.add(k2.set(5, 6, 7, 8))

    //remove a key
    val oc4: Observe = oc2.remove(k4)

    //#observe

    //validations
    assert(tryParam1.isSuccess)
    assert(tryk2Bad.isFailure)
    assert(paramSize === 4)
    assert(oc3(k2).values === Array(5, 6, 7, 8))
    assert(oc4.size === 3)
  }

  it("Should show usage of Wait command") {
    //#wait
    //keys
    val k1: Key[Boolean] = KeyType.BooleanKey.make("repeat")
    val k2: Key[Int]     = KeyType.IntKey.make("expTime")
    val k2bad: Key[Int]  = KeyType.IntKey.make("stringThing")
    val k3: Key[Int]     = KeyType.IntKey.make("filter")
    val k4: Key[Instant] = KeyType.TimestampKey.make("creation-time")

    //prefix
    val prefix: Prefix = Prefix("wfos.red.detector")

    //parameters
    val i1: Parameter[Boolean] = k1.set(true, false, true, false)
    val i2: Parameter[Int]     = k2.set(1, 2, 3, 4)

    //create wait, add sequentially using add
    val wc1: Wait = Wait(obsId, prefix).add(i1).add(i2)

    //access params using get method
    val k1Param: Option[Parameter[Boolean]] = wc1.get(k1)
    val values: Array[Boolean]              = k1Param.map(_.values).getOrElse(Array.empty[Boolean])

    //access parameters
    val tryParam1: Try[Parameter[Boolean]] = Try(wc1(k1))    //success
    val tryk2Bad: Try[Parameter[Int]]      = Try(wc1(k2bad)) //failure

    //add more than one parameters, using madd
    val wc2: Wait      = wc1.madd(k3.set(1, 2, 3, 4).withUnits(Units.day), k4.set(Instant.now()))
    val paramSize: Int = wc2.size

    //update existing key with set
    val wc3: Wait = wc1.add(k2.set(5, 6, 7, 8))

    //remove a key
    val wc4: Wait = wc2.remove(k4)

    //#wait

    //validations
    assert(tryParam1.isSuccess)
    assert(tryk2Bad.isFailure)
    assert(paramSize === 4)
    assert(wc3(k2).values === Array(5, 6, 7, 8))
    assert(wc4.size === 3)
  }
}
