package csw.database.client
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.jooq.DSLContext
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.time.{Seconds, Span}

object DBTestHelper {
  def postgres(): EmbeddedPostgres =
    EmbeddedPostgres.builder
      .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
      .setCleanDataDirectory(true)
      .setPgBinaryResolver(new PostgresBinaryResolver)
      .start

  def dslContext(system: ActorSystem, port: Int): DSLContext =
    new DatabaseServiceFactory(system.toTyped).makeDsl(port).futureValue(Interval(Span(5, Seconds)))
}
