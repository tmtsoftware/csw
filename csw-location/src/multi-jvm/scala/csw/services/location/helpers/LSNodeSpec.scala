package csw.services.location.helpers

import akka.remote.testkit.{FlightRecordingSupport, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import csw.services.location.commons.CswCluster
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers, Outcome}

abstract class LSNodeSpec[T <: NMembersAndSeed](val config: T)
    extends MultiNodeSpec(config, config.makeSystem)
    with ImplicitSender
    with MultiNodeSpecCallbacks
    with FunSuiteLike
    with Matchers
    with BeforeAndAfterAll {

  protected val cswCluster: CswCluster           = CswCluster.withSystem(system)
  protected val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  test("ensure that location service is up for all the nodes") {
    locationService.list.await
    enterBarrier("cluster-formed")
  }
}

abstract class LSNodeSpecForPerf[T <: NMembersAndSeed](override val config: T)
    extends LSNodeSpec(config)
    with FlightRecordingSupport {
  // Keep track of failure so we can print artery flight recording on failure
  private var failed = false
  final override protected def withFixture(test: NoArgTest): Outcome = {
    val out = super.withFixture(test)
    if (!out.isSucceeded)
      failed = true
    out
  }

  override def afterTermination(): Unit = {
    if (failed || sys.props.get("akka.remote.artery.always-dump-flight-recorder").isDefined) {
      printFlightRecording()
    }
    deleteFlightRecorderFile()
  }
}
