package csw.messages.params.generics

import java.io.Serializable
import java.time.Instant

import csw.messages.TMTSerializable
import csw.messages.commands._
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.messages.params.generics.KeyType.ChoiceKey
import csw.messages.params.models.{Choice, ObsId, Prefix}
import csw.messages.params.states.{CurrentState, CurrentStates, StateName}
import org.scalatest.FunSuite

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-187: Efficient serialization to/from binary
// DEOPSCSW-282: Add a timestamp Key and Parameter
class SerializationTest extends FunSuite {

  val obsId      = ObsId("2023-Q22-4-33")
  val fqn1       = "tcs.base.pos.name"
  val fqn1prefix = Prefix("tcs.base.pos")
  val fqn1name   = "name"
  val fqn2       = "tcs.base.pos.ra"
  val fqn3       = "tcs.base.pos.dec"

  val exposureTime: Key[Double] = KeyType.DoubleKey.make("exposureTime")
  val repeats: Key[Int]         = KeyType.IntKey.make("repeats")
  val ra: Key[String]           = KeyType.StringKey.make("ra")
  val dec: Key[String]          = KeyType.StringKey.make("dec")
  val epoch: Key[Double]        = KeyType.DoubleKey.make("epoch")
  val test: Key[Int]            = KeyType.IntKey.make("test")
  val timestamp: Key[Instant]   = KeyType.TimestampKey.make("ts")

  val sc1: Setup = Setup(Prefix("tcs.pos"), CommandName("move"), Some(obsId)).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1),
    timestamp.set(Instant.now)
  ) //.second

  val cs1: CurrentState = CurrentState(Prefix("tcs.pos"), StateName("testStateName")).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1),
    timestamp.set(Instant.now)
  ) //.second

  val cs2: CurrentState = CurrentState(Prefix("tcs.pos"), StateName("testStateName2")).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1),
    timestamp.set(Instant.now)
  )

  val submitCommandChoice   = Choice("SubmitCommand")
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", submitCommandChoice)
  val cs3: CurrentState     = CurrentState(Prefix("tcs.pos"), StateName("testStateName3")).add(choiceKey.set(submitCommandChoice))

  val disperser: Key[String] = KeyType.StringKey.make("disperser")
  val filter1: Key[String]   = KeyType.StringKey.make("filter1")
  val sc2: Setup = Setup(Prefix("wfos.blue"), CommandName("move"), Some(obsId))
    .add(disperser.set("gr243"))
    .add(filter1.set("GG433"))

  val ob1: Observe = Observe(Prefix("wfos.blue.camera"), CommandName("move"), Some(obsId))
    .add(exposureTime.set(22.3)) // .sec,
    .add(repeats.set(3))

  val wc1: Wait = Wait(Prefix("wfos.blue.camera"), CommandName("move"), Some(obsId))

  test("ConfigType kryo serialization") {
    import csw.messages.params.generics.ParamSetSerializer._

    val bytes = write(sc1)
    val scout = read[Setup](bytes)
    assert(scout == sc1)

    val bytes1 = write(ob1)
    val obout  = read[Observe](bytes1)
    assert(obout == ob1)

    val bytes2 = write(wc1)
    val wout   = read[Wait](bytes2)
    assert(wout == wc1)

    val bytes3 = write(cs1)
    val csout  = read[CurrentState](bytes3)
    assert(csout == cs1)
  }

  test("System event kryo serialization") {
    import csw.messages.params.generics.ParamSetSerializer._
    val event = SystemEvent(fqn1prefix, EventName("filter wheel"))
      .add(ra.set("12:32:11"))
      .add(dec.set("30:22:22"))

    val bytes1 = write(event)

    val out1 = read[Event](bytes1)
    assert(out1 == event)
  }

  test("CurrentStates kryo serialization") {
    import csw.messages.params.generics.ParamSetSerializer._

    val sca1   = CurrentStates(List(cs1))
    val bytes1 = write(sca1)

    val sout1 = read[CurrentStates](bytes1)
    assert(sout1 == sca1)
  }

//  test("SubscriptionKey kryo serialization") {
//    import csw.messages.params.generics.ParamSetSerializer._
//
//    val sk1: SubscriptionKey[CurrentState]    = SubscriptionKey.all[CurrentState]
//    val bytes1: Array[Byte]                   = write(sk1)
//    val skout1: SubscriptionKey[CurrentState] = read[SubscriptionKey[CurrentState]](bytes1)
//    assert(skout1.predicate(cs1))
//
//    val sk2: SubscriptionKey[CurrentState]    = new SubscriptionKey[CurrentState](_.stateName == StateName("testStateName2"))
//    val bytes2: Array[Byte]                   = write(sk2)
//    val skout2: SubscriptionKey[CurrentState] = read[SubscriptionKey[CurrentState]](bytes2)
//    assert(!skout2.predicate(cs1))
//    assert(skout2.predicate(cs2))
//
//    val sk3: SubscriptionKey[CurrentState] = new SubscriptionKey[CurrentState](state => {
//      val submitCommandChoice   = Choice("SubmitCommand")
//      val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", submitCommandChoice)
//      val choiceParameter       = choiceKey.set(submitCommandChoice)
//      state.paramSet.contains(choiceParameter)
//    })
//    val bytes3: Array[Byte]                   = write(sk3)
//    val skout3: SubscriptionKey[CurrentState] = read[SubscriptionKey[CurrentState]](bytes3)
//    assert(skout3.predicate(cs3))
//  }

}
