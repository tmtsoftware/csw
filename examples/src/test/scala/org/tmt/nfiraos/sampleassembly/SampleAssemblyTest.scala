package org.tmt.nfiraos.sampleassembly

import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import org.scalatest.FunSuiteLike

import scala.concurrent.Await

//#intro
class SampleAssemblyTest extends ScalaTestFrameworkTestKit(AlarmServer, EventServer) with FunSuiteLike {
  import frameworkTestKit.frameworkWiring._
//#intro

  //#setup
  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnStandalone(com.typesafe.config.ConfigFactory.load("SampleAssemblyStandalone.conf"))
  }
//#setup

  //#locate
  import scala.concurrent.duration._
  test("Assembly should be locatable using Location Service") {
    val connection   = AkkaConnection(ComponentId("SampleAssembly", ComponentType.Assembly))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    akkaLocation.connection shouldBe connection
  }
  //#locate
}
