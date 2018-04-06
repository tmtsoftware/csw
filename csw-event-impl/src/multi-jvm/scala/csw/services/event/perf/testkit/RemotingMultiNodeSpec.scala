package csw.services.event.perf.testkit

import akka.remote.testkit.{FlightRecordingSupport, MultiNodeConfig, MultiNodeSpec}
import akka.testkit.{DefaultTimeout, ImplicitSender}
import org.scalatest.{Outcome, Suite}

abstract class RemotingMultiNodeSpec(config: MultiNodeConfig)
    extends MultiNodeSpec(config)
    with Suite
    with STMultiNodeSpec
    with FlightRecordingSupport
    with ImplicitSender
    with DefaultTimeout { self: MultiNodeSpec ⇒

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
