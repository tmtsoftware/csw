package csw.param.events

import java.time.Instant

import csw.param.models.{ObsId, Prefix}
import csw.param.generics.KeyType
import csw.units.Units.meters
import org.scalatest.{FunSpec, Matchers}

// DEOPSCSW-183: Configure attributes and values
// DEOPSCSW-185: Easy to Use Syntax/Api
class EventsTest extends FunSpec with Matchers {
  private val s1: String = "encoder"

  private val s1Key = KeyType.IntKey.make(s1)

  private val ck = "wfos.blue.filter"

  describe("Check equal for EventInfo") {
    val ck1: Prefix = Prefix(ck)
    val testtime    = EventTime()

    it("should be equals since case class") {

      val ei1 = EventInfo(ck1, testtime, None)

      val ei2 = EventInfo(ck1, testtime, None)

      ei1 should equal(ei2)
    }

    it("should work with obsid too") {
      val obsID1 = ObsId("2022A-Q-P123-O456-F7890")

      val ei1 = EventInfo(ck1, testtime, Some(obsID1))

      val ei2 = EventInfo(ck1, testtime, Some(obsID1))

      ei1 should equal(ei2)
    }
  }

  describe("StatusEvent Test") {
    val k1  = KeyType.IntKey.make("encoder")
    val k2  = KeyType.IntKey.make("windspeed")
    val k3  = KeyType.IntKey.make("notUsed")
    val ck1 = "wfos.prog.cloudcover"
    val ck3 = "wfos.red.detector"

    it("Should allow adding keys") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = StatusEvent(ck3).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("Should allow setting") {
      var sc1 = StatusEvent(ck1)
      sc1 = sc1.add(k1.set(22)).add(k2.set(44))
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
    }

    it("Should allow apply") {
      var sc1 = StatusEvent(ck1)
      sc1 = sc1.add(k1.set(22)).add(k2.set(44))

      val v1 = sc1(k1)
      val v2 = sc1(k2)
      assert(sc1.get(k1).isDefined)
      assert(sc1.get(k2).isDefined)
      assert(v1.values === Array(22))
      assert(v2.values(0) == 44)
    }

    it("Should allow removing") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = StatusEvent(ck3).madd(i1, i2)

      assert(sc1.size == 2)
      assert(sc1.exists(k1))

      val mutatedSc1 = sc1.remove(k1)

      assert(!mutatedSc1.exists(k1))
      assert(mutatedSc1.size == 1)
    }

    // DEOPSCSW-190: Implement Unit Support
    it("should update for the same key with set") {
      var sc1 = StatusEvent(ck1)
      sc1 = sc1.add(k2.set(22))
      assert(sc1.exists(k2))
      assert(sc1(k2).values === Array(22))

      sc1 = sc1.add(k2.set(33).withUnits(meters))
      assert(sc1.exists(k2))
      assert(sc1(k2).units == meters)
      assert(sc1(k2).values === Array(33))
    }
  }

  describe("ObserveEvent Test") {
    val k1     = KeyType.IntKey.make("encoder")
    val k2     = KeyType.IntKey.make("windspeed")
    val k3     = KeyType.IntKey.make("notUsed")
    val prefix = "wfos.prog.cloudcover"

    it("should create with prefix and eventime") {
      val i1        = k1.set(22)
      val i2        = k2.set(44)
      val eventTime = Instant.now()

      val sc1 = ObserveEvent(prefix, eventTime).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("should create with prefix, eventime and obsId") {
      val i1        = k1.set(22)
      val i2        = k2.set(44)
      val eventTime = Instant.now()
      val obsId     = ObsId("obsId")

      val sc1 = ObserveEvent(prefix, eventTime, obsId).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(obsId == sc1.obsIdOption.get)
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("should create with event info") {
      val i1        = k1.set(22)
      val i2        = k2.set(44)
      val eventTime = Instant.now()
      val uuid      = "12345"
      val eventInfo = EventInfo(Prefix(prefix), eventTime, Some(ObsId("obsId")), uuid)

      val sc1 = ObserveEvent(eventInfo).madd(i1, i2)
      assert(sc1.size == 2)
      assert(eventInfo == sc1.info)
      assert(uuid == sc1.eventId)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("Should allow removing") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val oc1 = ObserveEvent(prefix).madd(i1, i2)

      assert(oc1.size == 2)
      assert(oc1.exists(k1))

      val mutatedOc1 = oc1.remove(k1)

      assert(!mutatedOc1.exists(k1))
      assert(mutatedOc1.size == 1)
    }
  }

  describe("SystemEvent Test") {
    val k1     = KeyType.IntKey.make("encoder")
    val k2     = KeyType.IntKey.make("windspeed")
    val k3     = KeyType.IntKey.make("notUsed")
    val prefix = "wfos.prog.cloudcover"

    it("should create with prefix and eventime") {
      val i1        = k1.set(22)
      val i2        = k2.set(44)
      val eventTime = Instant.now()

      val sc1 = SystemEvent(prefix, eventTime).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("should create with prefix, eventime and obsId") {
      val i1        = k1.set(22)
      val i2        = k2.set(44)
      val eventTime = Instant.now()
      val obsId     = ObsId("obsId")

      val sc1 = SystemEvent(prefix, eventTime, obsId).madd(i1, i2)
      assert(sc1.size == 2)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(obsId == sc1.obsIdOption.get)
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("should create with event info") {
      val i1        = k1.set(22)
      val i2        = k2.set(44)
      val eventTime = Instant.now()
      val eventInfo = EventInfo(prefix, eventTime)

      val sc1 = SystemEvent(eventInfo).madd(i1, i2)
      assert(sc1.size == 2)
      assert(eventInfo == sc1.info)
      assert(sc1.exists(k1))
      assert(sc1.exists(k2))
      assert(sc1(k1).head == 22)
      assert(sc1(k2).head == 44)
      assert(sc1.missingKeys(k1, k2, k3) == Set(k3.keyName))
    }

    it("Should allow removing") {
      val i1  = k1.set(22)
      val i2  = k2.set(44)
      val sc1 = SystemEvent(prefix).madd(i1, i2)

      assert(sc1.size == 2)
      assert(sc1.exists(k1))

      val mutatedSc1 = sc1.remove(k1)

      assert(!mutatedSc1.exists(k1))
      assert(mutatedSc1.size == 1)
    }
  }

  describe("Check equal for events") {
    it("should have equals working on SystemEvents") {

      val ev1 = SystemEvent(ck).add(s1Key -> 2)

      val ev2 = SystemEvent(ck).add(s1Key -> 2)

      val ev3 = SystemEvent(ck).add(s1Key -> 22)

      ev1.info should equal(ev2.info)

      ev1 should equal(ev2)
      ev1 should not equal (ev3)
    }
  }

}
