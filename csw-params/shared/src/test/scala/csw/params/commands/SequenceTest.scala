package csw.params.commands

import csw.params.core.models.Prefix
import org.scalatest.{Matchers, WordSpec}

class SequenceTest extends WordSpec with Matchers {

  "apply" must {
    "create sequence from provided list of commands" in {
      val setup    = Setup(Prefix("test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("test1"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      sequence.commands should ===(List(setup, observe))
    }
  }

  "add" must {
    "allow adding list of commands to existing sequence" in {
      val setup    = Setup(Prefix("test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("test2"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      val newSetup   = Setup(Prefix("test3"), CommandName("setup-test"), None)
      val newObserve = Observe(Prefix("test4"), CommandName("setup-test"), None)

      val updateSequence = sequence.add(newSetup, newObserve)
      updateSequence.commands should ===(List(setup, observe, newSetup, newObserve))
    }

    "allow adding new sequence to existing sequence" in {
      val setup    = Setup(Prefix("test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("test2"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      val newSetup    = Setup(Prefix("test3"), CommandName("setup-test"), None)
      val newObserve  = Observe(Prefix("test4"), CommandName("observe-test"), None)
      val newSequence = Sequence(newSetup, newObserve)

      val updateSequence = sequence.add(newSequence)
      updateSequence.commands should ===(List(setup, observe, newSetup, newObserve))
    }

  }
}
