package csw.database.commons

import java.nio.file.Paths

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.commons.ResourceReader
import csw.database.DatabaseServiceFactory
import csw.database.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.time.{Seconds, Span}

object DBTestHelper {
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

  def dslContext(system: ActorSystem[SpawnProtocol.Command], port: Int): DSLContext =
    dbServiceFactory(system)
      .makeDsl(port)
      .futureValue(Interval(Span(5, Seconds)))
}
