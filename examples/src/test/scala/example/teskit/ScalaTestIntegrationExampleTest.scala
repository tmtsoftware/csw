package example.teskit

//#scalatest-testkit
import com.typesafe.config.ConfigFactory
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

class ScalaTestIntegrationExampleTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyFunSuiteLike {

  test("test spawning component in standalone mode") {
    spawnStandalone(ConfigFactory.load("SampleHcdStandalone.conf"))

    // .. assertions etc.

  }
}
//#scalatest-testkit
