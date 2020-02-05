package org.tmt.nfiraos.sampleassembly

import csw.location.models.{ComponentId, ComponentType}
import csw.location.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.NFIRAOS
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
    val connection   = AkkaConnection(ComponentId(Prefix(NFIRAOS, "SampleAssembly"), ComponentType.Assembly))
    val akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get

    akkaLocation.connection shouldBe connection
  }
  //#locate
}