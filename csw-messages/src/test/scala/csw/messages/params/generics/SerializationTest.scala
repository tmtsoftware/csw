package csw.messages.params.generics

import java.time.Instant

import csw.messages.ccs.commands.{Observe, Setup, Wait}
import csw.messages.ccs.events.{EventServiceEvent, SystemEvent}
import csw.messages.params.models.{ObsId, Prefix, RunId}
import csw.messages.params.states.{CurrentState, CurrentStates}
import org.scalatest.FunSuite

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-187: Efficient serialization to/from binary
//DEOPSCSW-282: Add a timestamp Key and Parameter
class SerializationTest extends FunSuite {

  val obsId        = ObsId("2023-Q22-4-33")
  val runId: RunId = RunId()
  val fqn1         = "tcs.base.pos.name"
  val fqn1prefix   = "tcs.base.pos"
  val fqn1name     = "name"
  val fqn2         = "tcs.base.pos.ra"
  val fqn3         = "tcs.base.pos.dec"

  val exposureTime: Key[Double] = KeyType.DoubleKey.make("exposureTime")
  val repeats: Key[Int]         = KeyType.IntKey.make("repeats")
  val ra: Key[String]           = KeyType.StringKey.make("ra")
  val dec: Key[String]          = KeyType.StringKey.make("dec")
  val epoch: Key[Double]        = KeyType.DoubleKey.make("epoch")
  val test: Key[Int]            = KeyType.IntKey.make("test")
  val timestamp: Key[Instant]   = KeyType.TimestampKey.make("ts")

  val sc1: Setup = Setup(runId, obsId, Prefix("tcs.pos")).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1),
    timestamp.set(Instant.now)
  ) //.second

  val cs1: CurrentState = CurrentState(Prefix("tcs.pos")).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1),
    timestamp.set(Instant.now)
  ) //.second

  val disperser: Key[String] = KeyType.StringKey.make("disperser")
  val filter1: Key[String]   = KeyType.StringKey.make("filter1")
  val sc2: Setup = Setup(runId, obsId, Prefix("wfos.blue"))
    .add(disperser.set("gr243"))
    .add(filter1.set("GG433"))

  val ob1: Observe = Observe(runId, obsId, Prefix("wfos.blue.camera"))
    .add(exposureTime.set(22.3)) // .sec,
    .add(repeats.set(3))

  val wc1: Wait = Wait(runId, obsId, Prefix("wfos.blue.camera"))

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
    val event = SystemEvent(fqn1prefix)
      .add(ra.set("12:32:11"))
      .add(dec.set("30:22:22"))

    val bytes1 = write(event)

    val out1 = read[EventServiceEvent](bytes1)
    assert(out1 == event)
  }

  test("CurrentStates kryo serialization") {
    import csw.messages.params.generics.ParamSetSerializer._

    val sca1   = CurrentStates(List(cs1))
    val bytes1 = write(sca1)

    val sout1 = read[CurrentStates](bytes1)
    assert(sout1 == sca1)
  }

}
