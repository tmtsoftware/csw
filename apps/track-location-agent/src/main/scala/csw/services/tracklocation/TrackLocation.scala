package csw.services.tracklocation

import java.util.concurrent.atomic.AtomicBoolean

import akka.stream.scaladsl.{Sink, Source}
import csw.services.location.commons.CswCluster
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService
import csw.services.tracklocation.models.Command

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.sys.process._

/**
  * Starts a given external program, registers it with the location service and unregisters it when the program exits.
  */
class TrackLocation(names: List[String], command: Command, cswCluster: CswCluster, locationService: LocationService) {

  import cswCluster._

  private var isRunning = new AtomicBoolean(true)

  def run(): Future[Unit] = register().map(awaitTermination)

  /**
    * INTERNAL API : Registers the services from `names` collection with LocationService.
    */
  private def register(): Future[Seq[RegistrationResult]] = Source(names)
    .initialDelay(command.delay.millis) //delay to give the app a chance to start
    .mapAsync(1)(registerName)
    .runWith(Sink.seq)

  /**
    * INTERNAL API : Registers a single service as a TCP service.
    */
  private def registerName(name: String): Future[RegistrationResult] = {
    val componentId = ComponentId(name, ComponentType.Service)
    val connection = TcpConnection(componentId)
    locationService.register(TcpRegistration(connection, command.port))
  }

  /**
    * INTERNAL API : Registers a shutdownHook to handle service un-registration during abnormal exit. Then, executes user
    * specified command and awaits its termination.
    */
  private def awaitTermination(results: Seq[RegistrationResult]): Unit = {
    println(results.map(_.location.connection.componentId))

    cswCluster.coordinatedShutdown.addJvmShutdownHook {
      println("Shutdown hook reached, unregistering services.")
      unregisterServices(results)
      println(s"Exited the application.")
    }

    isRunning.set(true)
    println(s"Executing specified command: ${command.commandText}")
    val exitCode = command.commandText.!
    println(s"$command exited with exit code $exitCode")

    unregisterServices(results)

    Await.ready(locationService.shutdown(), 10.seconds)

    if (!command.noExit) System.exit(exitCode)
  }

  /**
    * INTERNAL API : Unregisters a service.
    */
  private def unregisterServices(results: Seq[RegistrationResult]): Unit = synchronized {
    if (isRunning.get()) {
      Await.result(Future.traverse(results)(_.unregister()), 10.seconds)
      isRunning.set(false)
      println(s"Services are unregistered.")
    }
  }
}
