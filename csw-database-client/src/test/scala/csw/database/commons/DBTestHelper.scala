package csw.database.commons

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.database.client.DatabaseServiceFactory
import csw.database.client.DatabaseServiceFactory.{ReadPasswordHolder, ReadUsernameHolder}
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.time.{Seconds, Span}

object DBTestHelper {
  def postgres(port: Int): EmbeddedPostgres =
    EmbeddedPostgres.builder
      .setServerConfig("listen_addresses", "*")
      .setServerConfig("hba_file", "src/test/resources/pg_hba.conf")
      .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
      .setCleanDataDirectory(true)
      .setPort(port)
      .start

  def dbServiceFactory(system: ActorSystem) =
    new DatabaseServiceFactory(system.toTyped, Map(ReadUsernameHolder → "postgres", ReadPasswordHolder → "postgres"))

  def dslContext(system: ActorSystem, port: Int): DSLContext =
    dbServiceFactory(system)
      .makeDsl(port)
      .futureValue(Interval(Span(5, Seconds)))
}
