package csw.messages.params.generics

import csw.messages.commands._
import csw.messages.events._
import csw.messages.params.models.{Id, ObsId, Prefix, Struct}
import csw.messages.params.states.{CurrentState, DemandState, StateName}
import org.scalatest.{FunSpec, Matchers}

//DEOPSCSW-184: Change configurations - attributes and values
class UniqueKeyVerificationTest extends FunSpec with Matchers {

  val encoderKey: Key[Int] = KeyType.IntKey.make("encoder")
  val filterKey: Key[Int]  = KeyType.IntKey.make("filter")
  val miscKey: Key[Int]    = KeyType.IntKey.make("misc.")

  val runId  = Id()
  val prefix = Prefix("wfos.blue.filter")

  private val encParam1 = encoderKey.set(1)
  private val encParam2 = encoderKey.set(2)
  private val encParam3 = encoderKey.set(3)

  private val filterParam1 = filterKey.set(1)
  private val filterParam2 = filterKey.set(2)
  private val filterParam3 = filterKey.set(3)

  private val miscParam1 = miscKey.set(100)

  describe("Test Commands") {

    it("Setup command is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val setup =
        Setup(prefix,
              CommandName("move"),
              Some(ObsId("Obs001")),
              Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      setup.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedSetup = setup.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedSetup.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalSetUp = setup.madd(Set(miscParam1, encParam1))
      finalSetUp.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                      filterKey.keyName,
                                                                                      miscKey.keyName)
    }

    it("Observe command is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val observe =
        Observe(prefix,
                CommandName("move"),
                Some(ObsId("Obs001")),
                Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      observe.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedObserve = observe.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedObserve.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalObserve = observe.madd(Set(miscParam1, encParam1))
      finalObserve.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                        filterKey.keyName,
                                                                                        miscKey.keyName)
    }

    it("Wait command is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val wait =
        Wait(prefix,
             CommandName("move"),
             Some(ObsId("Obs001")),
             Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      wait.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedWait = wait.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedWait.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalWait = wait.madd(Set(miscParam1, encParam1))
      finalWait.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                     filterKey.keyName,
                                                                                     miscKey.keyName)
    }
  }

  describe("Test Result") {

    it("is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val result =
        Result(prefix, Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      result.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedResult = result.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedResult.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalResult = result.madd(Set(miscParam1, encParam1))
      finalResult.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                       filterKey.keyName,
                                                                                       miscKey.keyName)
    }
  }

  describe("Test Struct") {

    it("is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val result = Struct(Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      result.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedResult = result.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedResult.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalResult = result.madd(Set(miscParam1, encParam1))
      finalResult.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                       filterKey.keyName,
                                                                                       miscKey.keyName)
    }
  }

  describe("Test StateVariables") {

    it("DemandState is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val state = DemandState("prefix",
                              StateName("testStateName"),
                              Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      state.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedState = state.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedState.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalState = state.madd(Set(miscParam1, encParam1))
      finalState.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                      filterKey.keyName,
                                                                                      miscKey.keyName)
    }

    it("CurrentState is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val state =
        CurrentState("prefix",
                     StateName("testStateName"),
                     Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      state.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedState = state.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedState.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalState = state.madd(Set(miscParam1, encParam1))
      finalState.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                      filterKey.keyName,
                                                                                      miscKey.keyName)
    }
  }

  describe("Test Events") {

    it("ObserveEvent command is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val event =
        ObserveEvent(prefix,
                     EventName("filter wheel"),
                     Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      event.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedEvent = event.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedEvent.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalEvent = event.madd(Set(miscParam1, encParam1))
      finalEvent.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                      filterKey.keyName,
                                                                                      miscKey.keyName)
    }

    it("SystemEvent command is able to remove duplicate keys") {

      //parameters with duplicate key via constructor
      val event =
        SystemEvent(prefix,
                    EventName("filter wheel"),
                    Set(encParam1, encParam2, encParam3, filterParam1, filterParam2, filterParam3))
      event.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameters with duplicate key via add + madd
      val changedEvent = event.add(encParam3).madd(filterParam1, filterParam2, filterParam3)
      changedEvent.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName, filterKey.keyName)

      //parameter with unique key and parameter with duplicate key
      val finalEvent = event.madd(Set(miscParam1, encParam1))
      finalEvent.paramSet.toList.map(_.keyName) should contain theSameElementsAs List(encoderKey.keyName,
                                                                                      filterKey.keyName,
                                                                                      miscKey.keyName)
    }
  }
}
