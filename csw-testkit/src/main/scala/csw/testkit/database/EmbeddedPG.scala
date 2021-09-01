package csw.testkit.database

import akka.actor.typed
import akka.util.Timeout
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import csw.commons.ResourceReader
import csw.database.commons.DatabaseServiceConnection
import csw.location.api.models.TcpRegistration
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.testkit.internal.TestKitUtils

import java.nio.file.Paths

trait EmbeddedPG {
  implicit def system: typed.ActorSystem[?]
  implicit def timeout: Timeout

  protected lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val connection                              = DatabaseServiceConnection.value
  private var _postgres: Option[EmbeddedPostgres]     = None

  protected def start(port: Int): RegistrationResult = {
    val postgres = EmbeddedPostgres.builder
      .setServerConfig("listen_addresses", "*")
      .setServerConfig("hba_file", ResourceReader.copyToTmp("/pg_hba.conf").toString)
      .setDataDirectory(Paths.get("/tmp/postgresDataDir"))
      .setCleanDataDirectory(true)
      .setPort(port)
      .start

    _postgres = Some(postgres)
    addJvmShutdownHook(shutdown())

    TestKitUtils.await(locationService.register(TcpRegistration(connection, port)), timeout)
  }

  protected def shutdown(): Unit = {
    try {
      _postgres.foreach { pg =>
        pg.close()
        TestKitUtils.await(locationService.unregister(connection), timeout)
      }
    }
    finally {
      system.terminate()
      TestKitUtils.await(system.whenTerminated, timeout)
    }
  }

  private def addJvmShutdownHook[T](hook: => T): Unit =
    Runtime.getRuntime.addShutdownHook(new Thread { override def run(): Unit = hook })

}
