package csw.param

import csw.param.Parameters._
import csw.param.Events.{EventServiceEvent, SystemEvent}
import csw.param.StateVariable._
import csw.param.parameters.primitives.{DoubleKey, IntKey, StringKey}
import org.scalatest.FunSuite

//noinspection TypeAnnotation
class SerializationTests extends FunSuite {

  val obsId       = ObsId("2023-Q22-4-33")
  val commandInfo = CommandInfo(obsId)

  val fqn1       = "tcs.base.pos.name"
  val fqn1prefix = "tcs.base.pos"
  val fqn1name   = "name"
  val fqn2       = "tcs.base.pos.ra"
  val fqn3       = "tcs.base.pos.dec"

  val exposureTime = DoubleKey("exposureTime")
  val repeats      = IntKey("repeats")
  val ra           = StringKey("ra")
  val dec          = StringKey("dec")
  val epoch        = DoubleKey("epoch")
  val test         = IntKey("test")

  val sc1 = Setup(commandInfo, "tcs.pos").madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1)
  ) //.second

  val cs1 = CurrentState("tcs.pos").madd(
    ra.set("12:32:11"),
    dec.set("30:22:22"),
    epoch.set(1950.0),
    test.set(1)
  ) //.second

  val disperser = StringKey("disperser")
  val filter1   = StringKey("filter1")
  val sc2 = Setup(commandInfo, "wfos.blue")
    .add(disperser.set("gr243"))
    .add(filter1.set("GG433"))

  val ob1 = Observe(commandInfo, "wfos.blue.camera")
    .add(exposureTime.set(22.3)) // .sec,
    .add(repeats.set(3))

  val wc1 = Wait(commandInfo, "wfos.blue.camera")

  test("ConfigType Java serialization") {
    import ParamSetSerializer._

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
    import ParamSetSerializer._
    val event = SystemEvent(fqn1prefix)
      .add(ra.set("12:32:11"))
      .add(dec.set("30:22:22"))

    val bytes1 = write(event)

    val out1 = read[EventServiceEvent](bytes1)
    assert(out1 == event)
  }

  test("CurrentStates Java serialization") {
    import ParamSetSerializer._

    val sca1   = CurrentStates(List(cs1))
    val bytes1 = write(sca1)

    val sout1 = read[CurrentStates](bytes1)
    assert(sout1 == sca1)
  }

}
