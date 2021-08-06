package example.tutorial.basic

import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.funsuite.AnyFunSuiteLike

import scala.concurrent.Await

//#intro
class BasicSampleAssemblyTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with AnyFunSuiteLike {
  import frameworkTestKit._
//#intro

  //#setup
  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("BasicSampleAssemblyStandalone.conf"))
  }
//#setup

  //#locate
  import scala.concurrent.duration._
  test("Assembly should be locatable using Location Service") {
    val connection   = AkkaConnection(ComponentId(Prefix(Subsystem.CSW, "sample"), ComponentType.Assembly))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    akkaLocation.connection shouldBe connection
  }
  //#locate
}
