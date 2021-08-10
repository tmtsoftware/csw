package csw.params.events

import csw.params.core.models.ExposureId
import csw.prefix.models.Prefix
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.*

class DMSObserveEventTest extends AnyFunSpec with Matchers {
  describe("DMS Observe event") {
    val sourcePrefix           = Prefix("DMS.Metadata")
    val exposureId: ExposureId = ExposureId("2022A-001-123-IRIS-IMG-DRK1-0023")

    it("should create observe event with exposure id parameters | CSW-156") {
      Table(
        ("Observe event", "event name", "prefix"),
        (DMSObserveEvent.metadataAvailable(exposureId), "ObserveEvent.MetadataAvailable", sourcePrefix),
        (DMSObserveEvent.exposureAvailable(exposureId), "ObserveEvent.ExposureAvailable", sourcePrefix)
      ).forEvery((observeEvent, eventName, sourcePrefix) => {
        observeEvent.eventName.name shouldBe eventName
        observeEvent.source shouldBe sourcePrefix
      })
    }
  }
}
