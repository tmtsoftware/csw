package csw.services.tracklocation

import akka.Done
import akka.actor.CoordinatedShutdown
import csw.services.location.commons.CswCluster
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.models.Command

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.sys.process._

/**
 * Starts a given external program, registers it with the location service and unregisters it when the program exits.
 */
class TrackLocation(names: List[String], command: Command, cswCluster: CswCluster) {

  import cswCluster._
  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  def run(): Done = {
    Thread.sleep(command.delay)
    //Register all connections
    val results = Await.result(Future.traverse(names)(registerName), 10.seconds)
    unregisterOnTermination(results)
    Done
  }

  /**
   * INTERNAL API : Registers a single service as a TCP service.
   */
  private def registerName(name: String): Future[RegistrationResult] = {
    val componentId = ComponentId(name, ComponentType.Service)
    val connection  = TcpConnection(componentId)
    locationService.register(TcpRegistration(connection, command.port))
  }

  /**
   * INTERNAL API : Registers a shutdownHook to handle service un-registration during abnormal exit. Then, executes user
   * specified command and awaits its termination.
   */
  private def unregisterOnTermination(results: Seq[RegistrationResult]): Unit = {
    println(results.map(_.location.connection.componentId))

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "unregistering"
    )(() => unregisterServices(results))

    println(s"Executing specified command: ${command.commandText}")
    val exitCode = command.commandText.!
    println(s"$command exited with exit code $exitCode")
  }

  /**
   * INTERNAL API : Unregisters a service.
   */
  private def unregisterServices(results: Seq[RegistrationResult]): Future[Done] = {
    println("Shutdown hook reached, unregistering services.")
    Future.traverse(results)(_.unregister()).map { _ =>
      println(s"Services are unregistered.")
      Done
    }
  }
}
