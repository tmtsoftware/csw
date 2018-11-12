package csw.testkit.scaladsl
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.config.client.commons.ConfigServiceConnection
import csw.event.client.internal.commons.EventServiceConnection
import csw.testkit.scaladsl.CSWService.{ConfigServer, EventServer, LocationServer}
import org.scalatest.FunSuiteLike

// DEOPSCSW-592: Create csw testkit for component writers
class ScalaTestFrameworkTestKitSpec
    extends ScalaTestFrameworkTestKit(LocationServer, ConfigServer, EventServer)
    with FunSuiteLike {

  import frameworkTestKit.frameworkWiring._

  test("should start all the provided services") {

    val configLocation = locationService.find(ConfigServiceConnection.value).futureValue.value
    configLocation.connection shouldBe ConfigServiceConnection.value

    val eventLocation = locationService.find(EventServiceConnection.value).futureValue.value
    eventLocation.connection shouldBe EventServiceConnection.value

    // AlarmServer is not provided in ScalaTestFrameworkTestKit constructor, hence it should not be started
    val alarmLocation = locationService.find(AlarmServiceConnection.value).futureValue
    alarmLocation shouldBe None

  }

}
