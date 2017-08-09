package csw.param.generics

import csw.param.commands.{CommandInfo, Observe, Setup, Wait}
import csw.param.states.{CurrentState, CurrentStates}
import csw.param.events.{EventServiceEvent, SystemEvent}
import csw.param.models.{ObsId, Prefix}
import org.scalatest.FunSuite

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-187: Efficient serialization to/from binary
class SerializationTest extends FunSuite {

  val obsId       = ObsId("2023-Q22-4-33")
  val commandInfo = CommandInfo(obsId)

  val fqn1       = "tcs.base.pos.name"
  val fqn1prefix = "tcs.base.pos"
  val fqn1name   = "name"
  val fqn2       = "tcs.base.pos.ra"
  val fqn3       = "tcs.base.pos.dec"

  val exposureTime = KeyType.DoubleKey.make("exposureTime")
  val repeats      = KeyType.IntKey.make("repeats")
  val ra           = KeyType.StringKey.make("ra")
  val dec          = KeyType.StringKey.make("dec")
  val epoch        = KeyType.DoubleKey.make("epoch")
  val test         = KeyType.IntKey.make("test")

  val sc1 = Setup(commandInfo, Prefix("tcs.pos")).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1)
  ) //.second

  val cs1 = CurrentState(Prefix("tcs.pos")).madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1)
  ) //.second

  val disperser = KeyType.StringKey.make("disperser")
  val filter1   = KeyType.StringKey.make("filter1")
  val sc2 = Setup(commandInfo, Prefix("wfos.blue"))
    .add(disperser.set("gr243"))
    .add(filter1.set("GG433"))

  val ob1 = Observe(commandInfo, Prefix("wfos.blue.camera"))
    .add(exposureTime.set(22.3)) // .sec,
    .add(repeats.set(3))

  val wc1 = Wait(commandInfo, Prefix("wfos.blue.camera"))

  test("ConfigType Java serialization") {
    import csw.param.generics.ParamSetSerializer._

    // Test setup config Java serialization
    val bytes = write(sc1)
    val scout = read[Setup](bytes)
    assert(scout == sc1)

    // Test observe config Java serialization
    val bytes1 = write(ob1)
    val obout  = read[Observe](bytes1)
    assert(obout == ob1)

    // Test wait config Java serialization
    val bytes2 = write(wc1)
    val wout   = read[Wait](bytes2)
    assert(wout == wc1)

    // Test current state Java serialization
    val bytes3 = write(cs1)
    val csout  = read[CurrentState](bytes3)
    assert(csout == cs1)
  }

  test("Base trait event Java serialization") {
    import csw.param.generics.ParamSetSerializer._
    val event = SystemEvent(fqn1prefix)
      .add(ra.set("12:32:11"))
      .add(dec.set("30:22:22"))

    val bytes1 = write(event)

    val out1 = read[EventServiceEvent](bytes1)
    assert(out1 == event)
  }

  test("CurrentStates Java serialization") {
    import csw.param.generics.ParamSetSerializer._

    val sca1   = CurrentStates(List(cs1))
    val bytes1 = write(sca1)

    val sout1 = read[CurrentStates](bytes1)
    assert(sout1 == sca1)
  }

}
