/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.aas.installed

import java.nio.file.Paths

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.aas.installed.scaladsl.FileAuthStore
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

import scala.concurrent.duration.DurationLong

object Example extends App {

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  import actorSystem._

  val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  val installedAuthAdapter = InstalledAppAuthAdapterFactory.make(locationService, new FileAuthStore(Paths.get("/tmp/auth")))

  println("login initiated")
  installedAuthAdapter.login()

  private val expires: Long = installedAuthAdapter.getAccessToken().get.exp.get
  println(s"Expiring on: $expires")
  println(System.currentTimeMillis() / 1000)

  private val timeLeft: Long = expires - System.currentTimeMillis() / 1000
  println(s"time left to expire: $timeLeft")

  println(installedAuthAdapter.getAccessToken())

  println(installedAuthAdapter.getAccessToken((timeLeft + 100).seconds))
}
