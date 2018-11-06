package csw.teskit

//#scalatest-testkit
import com.typesafe.config.ConfigFactory
import csw.testkit.scaladsl.CSWService.{AlarmStore, EventStore}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.FunSuiteLike

class ScalaTestIntegrationExampleTest extends ScalaTestFrameworkTestKit(AlarmStore, EventStore) with FunSuiteLike {

  test("test spawning component in standalone mode") {
    spawnStandalone(ConfigFactory.load("SampleHcdStandalone.conf"))

    // .. assertions etc.

  }

}
//#scalatest-testkit
