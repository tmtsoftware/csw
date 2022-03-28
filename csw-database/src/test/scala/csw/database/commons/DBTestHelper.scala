/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.database.commons

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.commons.ResourceReader
import csw.database.DatabaseServiceFactory
import csw.database.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.jooq.DSLContext
import org.scalatest.concurrent.ScalaFutures

import java.nio.file.Paths
import scala.concurrent.duration.DurationInt

object DBTestHelper extends ScalaFutures {
  def postgres(port: Int): EmbeddedPostgres =
    EmbeddedPostgres.builder
      .setServerConfig("listen_addresses", "*")
      .setServerConfig("hba_file", ResourceReader.copyToTmp("/pg_hba.conf").toString)
      .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
      .setCleanDataDirectory(true)
      .setPort(port)
      .start

  def dbServiceFactory(system: ActorSystem[SpawnProtocol.Command]) =
    new DatabaseServiceFactory(system, Map(ReadUsernameHolder -> "postgres", ReadPasswordHolder -> "postgres"))

  def dslContext(system: ActorSystem[SpawnProtocol.Command], port: Int): DSLContext = {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(5.seconds)
    dbServiceFactory(system)
      .makeDsl(port)
      .futureValue
  }
}
