//package csw.util.config
//
//import org.scalatest.FunSpec
//
///**
// * Tests the config classes
// */
//class Config2Tests extends FunSpec {
//  val fqn1 = "tcs.base.pos.name"
//  val fqn1prefix = "tcs.base.pos"
//  val fqn1name = "name"
//  val fqn2 = "tcs.base.pos.ra"
//  // tcs/base/pos/ra
//  val fqn3 = "tcs.base.pos.dec"
//  val fqn4 = "tcs.wfs1.pos.name"
//  val fqn5 = "tcs.wfs2.pos.ra"
//  val fqn6 = "tcs.wfs2.pos.dec"
//  val fqn7 = "mobie.red.filter"
//  val fqn8 = "mobie.red.filter2"
//
//  val testUnits1 = Seconds
//  val testUnits2 = Meters
//
//  val testObsId = "2020-Q22-2-3"
//  //  val testConfigInfo = ConfigInfo(ObsID(testObsId))
//
//  /**
//   * Tests for ValueData
//   */
//  describe("Basic key value tests") {
//
//    val ck = "wfos.blue.filter"
//    val ck1 = "wfos.prog.cloudcover"
//    val ck2 = "wfos.red.filter"
//    val ck3 = "wfos.red.detector"
//
//    var sc1: Setup = Setup(ck)
//    sc1 = sc1.set(position, "GG484")
//    it("should have size 1") {
//      assert(sc1.size === 1)
//    }
//    it("should have the Some value") {
//      assert(sc1.get(position) === Some("GG484"))
//    }
//    it("should have None value") {
//      assert(sc1.get(exposureClass) === None)
//    }
//
//    it("should work with a second instance as well") {
//      val sc2 = Setup(ck2)
//        .set(position, "IR2")
//        .set(cloudCover, PERCENT_20)
//      assert(sc2(position) === "IR2")
//      assert(sc2(cloudCover) === PERCENT_20)
//
//      // Check option version too
//      assert(sc2.get(position) == Some("IR2"))
//      assert(sc2.get(cloudCover) == Some(PERCENT_20))
//    }
//
//    val ob1 = Observe(ck3)
//      .set(exposureTime, 22)
//      .set(repeats, 2)
//      .set(exposureType, OBSERVE)
//      .set(exposureClass, SCIENCE)
//    assert(ob1(exposureTime) === 22.0)
//    assert(ob1(repeats) === 2)
//    assert(ob1(exposureType) === OBSERVE)
//    assert(ob1(exposureClass) === SCIENCE)
//
//    // Check get of a non-existent key
//    it("should result in a NoSuchElementException") {
//      intercept[NoSuchElementException] {
//        val test = ob1(position)
//      }
//    }
//
//    // Test sequence Key type
//    it("should allow keys with sequences") {
//      val arrayKey = Key.create[Seq[Int]]("arrayKey")
//
//      val value = Seq(1, 2, 3, 4)
//      val sc1 = Setup(ck1).set(arrayKey, value)
//      assert(sc1.size == 1)
//      assert(sc1(arrayKey) === value)
//    }
//  }
//
//  describe("Filters") {
//
//    val ck = "wfos.blue.filter"
//    val ck2 = "wfos.red.filter"
//    val ck3 = "iris.imager.detector"
//
//    val sc1: Setup = Setup(ck)
//    val sc2 = Setup(ck2)
//    val ob1 = Observe(ck3)
//    val w1 = Wait(ck3)
//    val s1 = Seq(sc1, sc2, ob1, w1)
//    //println("Sca1: " + s1)
//
//    it("should see 3 prefixes - one is duplicate") {
//      val r1 = ConfigFilters.prefixes(s1)
//      assert(r1.size == 3)
//    }
//
//    it("should see 2 setup configs") {
//      val r1 = ConfigFilters.onlySetups(s1)
//      assert(r1.size == 2)
//    }
//
//    it("should see 1 observe configs") {
//      val r1 = ConfigFilters.onlyObserves(s1)
//      assert(r1.size == 1)
//    }
//
//    it("should see 1 wait config") {
//      val r1 = ConfigFilters.onlyWaits(s1)
//      assert(r1.size == 1)
//    }
//
//    // Prefix starts with tests
//    it("should have 2 starting with wfos") {
//      val r1 = ConfigFilters.prefixStartsWith("wfos", s1)
//      assert(r1.size == 2)
//    }
//    it("should have 2 with starting with iris") {
//      val r1 = ConfigFilters.prefixStartsWith("iris", s1)
//      assert(r1.size == 2)
//    }
//    it("should have 0 starting with Kim") {
//      val r1 = ConfigFilters.prefixStartsWith("Kim", s1)
//      assert(r1.size == 0)
//    }
//    it("should have 2 that contain filter") {
//      val r1 = ConfigFilters.prefixContains("filter", s1)
//      assert(r1.size == 2)
//    }
//    it("should have 0 that contain Allan") {
//      val r1 = ConfigFilters.prefixContains("Allan", s1)
//      assert(r1.size == 0)
//    }
//
//    // Subsystem filter
//    it("should have 2 with sub wfos") {
//      val r1 = ConfigFilters.prefixIsSubsystem(WFOS, s1)
//      assert(r1.size == 2)
//    }
//    it("should have 2 with sub iris") {
//      val r1 = ConfigFilters.prefixIsSubsystem(IRIS, s1)
//      assert(r1.size == 2)
//    }
//    it("should have 0 starting with AOESW") {
//      val r1 = ConfigFilters.prefixIsSubsystem(AOESW, s1)
//      assert(r1.size == 0)
//    }
//  }
//
//  describe("Testing config traits1") {
//
//    val ck = "wfos.blue.filter"
//    val ck2 = "wfos.red.filter"
//    val ck3 = "iris.imager.detector"
//
//    val sc1: Setup = Setup(ck)
//    val ob1 = Observe(ck3)
//    val w1 = Wait(ck3)
//
//    it("SequenceConfig should allow all three config types") {
//      assert(sc1.isInstanceOf[SequenceConfig])
//      assert(ob1.isInstanceOf[SequenceConfig])
//      assert(w1.isInstanceOf[SequenceConfig])
//    }
//
//    it("ControlConfig should not allow Wait") {
//      assert(sc1.isInstanceOf[ControlConfig])
//      assert(ob1.isInstanceOf[ControlConfig])
//      // assert(w1.isInstanceOf[ControlConfig] != true) // fruitless since Wait does in extend ControlConfig
//    }
//  }
//
//  describe("Basic key value removal tests") {
//
//    val ck = "wfos.blue.detector"
//
//    val ob1 = Observe(ck)
//      .set(exposureTime, 22)
//      .set(repeats, 2)
//      .set(exposureType, OBSERVE)
//      .set(exposureClass, SCIENCE)
//
//    it("Should have all the keys") {
//      assert(ob1(exposureTime) === 22.0)
//      assert(ob1(repeats) === 2)
//      assert(ob1(exposureType) === OBSERVE)
//      assert(ob1(exposureClass) === SCIENCE)
//    }
//
//    it("Should allow removal of keys") {
//      val ob2 = ob1.remove(exposureTime)
//      assert(ob2.get(exposureTime) == None)
//      assert(ob2.size == 3)
//      val ob3 = ob2.remove(exposureClass)
//      assert(ob3.get(exposureClass) == None)
//      assert(ob3.size == 2)
//      val ob4 = ob3.remove(exposureType)
//      assert(ob4.get(exposureType) == None)
//      assert(ob4.size == 1)
//      val ob5 = ob4.remove(repeats)
//      assert(ob5.get(repeats) == None)
//      assert(ob5.size == 0)
//    }
//
//  }
//
//}