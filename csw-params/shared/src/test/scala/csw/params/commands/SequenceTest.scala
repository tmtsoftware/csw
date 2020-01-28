package csw.params.commands

import csw.prefix.models.Prefix
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SequenceTest extends AnyFunSuite with Matchers {

  test("apply - create sequence from provided list of commands") {
    val setup    = Setup(Prefix("csw.setup"), CommandName("setup-test"), None)
    val observe  = Observe(Prefix("csw.observe"), CommandName("observe-test"), None)
    val sequence = Sequence(setup, observe)

    sequence.commands should ===(List(setup, observe))
  }

  test("add - allow adding list of commands to existing sequence") {
    val setup    = Setup(Prefix("csw.setup"), CommandName("setup-test"), None)
    val observe  = Observe(Prefix("csw.observe"), CommandName("observe-test"), None)
    val sequence = Sequence(setup, observe)

    val newSetup   = Setup(Prefix("csw.setup3"), CommandName("setup-test"), None)
    val newObserve = Observe(Prefix("csw.observe4"), CommandName("setup-test"), None)

    val updateSequence = sequence.add(newSetup, newObserve)
    updateSequence.commands should ===(List(setup, observe, newSetup, newObserve))
  }

  test("add - allow adding new sequence to existing sequence") {
    val setup    = Setup(Prefix("csw.setup"), CommandName("setup-test"), None)
    val observe  = Observe(Prefix("csw.observe"), CommandName("observe-test"), None)
    val sequence = Sequence(setup, observe)

    val newSetup    = Setup(Prefix("csw.setup3"), CommandName("setup-test"), None)
    val newObserve  = Observe(Prefix("csw.observe3"), CommandName("observe-test"), None)
    val newSequence = Sequence(newSetup, newObserve)

    val updateSequence = sequence.add(newSequence)
    updateSequence.commands should ===(List(setup, observe, newSetup, newObserve))
  }
}
