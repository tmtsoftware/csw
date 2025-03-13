/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location.models

import java.net.URI

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.exceptions.LocalPekkoActorRegistrationNotAllowed
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection, TcpConnection}
import csw.location.api.models.*
import csw.location.client.ActorSystemFactory
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.network.utils.Networks
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RegistrationTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "my-actor-1")

  test("should able to create the PekkoRegistration which should internally create PekkoLocation without metadata | CSW-108") {
    val hostname = Networks().hostname

    val pekkoConnection = PekkoConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
    val actorRef: ActorRef[?] = actorSystem.spawn(
      Behaviors.empty,
      "my-actor-3"
    )

    val pekkoRegistration     = PekkoRegistrationFactory.make(pekkoConnection, actorRef)
    val expectedPekkoLocation = PekkoLocation(pekkoConnection, actorRef.toURI, Metadata.empty)

    pekkoRegistration.location(hostname) shouldBe expectedPekkoLocation
  }

  test("should able to create the PekkoRegistration with metadata which should internally create PekkoLocation | CSW-108") {
    val hostname = Networks().hostname

    val pekkoConnection = PekkoConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))
    val actorRef: ActorRef[?] = actorSystem.spawn(
      Behaviors.empty,
      "my-actor-4"
    )

    val metadata              = Metadata(Map("key1" -> "value"))
    val pekkoRegistration     = PekkoRegistrationFactory.make(pekkoConnection, actorRef, metadata)
    val expectedPekkoLocation = PekkoLocation(pekkoConnection, actorRef.toURI, metadata)

    pekkoRegistration.location(hostname) shouldBe expectedPekkoLocation
  }

  test("should able to create the HttpRegistration which should internally create HttpLocation without metadata | CSW-108") {
    val hostname = Networks().hostname
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "trombone"), ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix)

    val expectedhttpLocation = HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"), Metadata.empty)

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test(
    "should able to create the HttpRegistration with metadata which should internally create HttpLocation with metadata | CSW-108"
  ) {
    val hostname = Networks().hostname
    val port     = 9595
    val prefix   = "/trombone/hcd"

    val httpConnection   = HttpConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "trombone"), ComponentType.HCD))
    val metadata         = Metadata(Map("key1" -> "value1"))
    val httpRegistration = HttpRegistration(httpConnection, port, prefix, metadata)

    val expectedhttpLocation =
      HttpLocation(httpConnection, new URI(s"http://$hostname:$port/$prefix"), metadata)

    httpRegistration.location(hostname) shouldBe expectedhttpLocation
  }

  test("should able to create the TcpRegistration which should internally create TcpLocation without metadata | CSW-108") {
    val hostname = Networks().hostname
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "lgsTrombone"), ComponentType.HCD))
    val tcpRegistration = TcpRegistration(tcpConnection, port)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"), Metadata.empty)

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should able to create the TcpRegistration with metadata which should internally create TcpLocation |  CSW-108") {
    val hostname = Networks().hostname
    val port     = 9596

    val tcpConnection   = TcpConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "lgsTrombone"), ComponentType.HCD))
    val metadata        = Metadata(Map("key1" -> "value1"))
    val tcpRegistration = TcpRegistration(tcpConnection, port, metadata)

    val expectedTcpLocation = TcpLocation(tcpConnection, new URI(s"tcp://$hostname:$port"), metadata)

    tcpRegistration.location(hostname) shouldBe expectedTcpLocation
  }

  test("should not allow PekkoRegistration using local ActorRef") {
    val config: Config = ConfigFactory.parseString("""
        pekko.actor.provider = local
      """)

    implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "local-actor-system", config)
    val actorRef                                                 = actorSystem.spawn(Behaviors.empty, "my-actor-2")
    val pekkoConnection = PekkoConnection(api.models.ComponentId(Prefix(Subsystem.NFIRAOS, "hcd1"), ComponentType.HCD))

    intercept[LocalPekkoActorRegistrationNotAllowed] {
      PekkoRegistrationFactory.make(pekkoConnection, actorRef)
    }
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }
}
