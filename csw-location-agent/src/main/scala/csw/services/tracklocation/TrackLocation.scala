package csw.services.tracklocation

import akka.Done
import akka.actor.CoordinatedShutdown
import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.commons.LocationAgentLogger
import csw.services.tracklocation.models.Command

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.control.NonFatal

/**
 * Starts a given external program, registers it with the location service and unregisters it when the program exits.
 */
class TrackLocation(names: List[String], command: Command, clusterSettings: ClusterSettings)
    extends LocationAgentLogger.Simple {

  private val cswCluster      = CswCluster.withSettings(clusterSettings)
  private val locationService = LocationServiceFactory.withCluster(cswCluster)

  import cswCluster._

  def run(): Process =
    try {
      Thread.sleep(command.delay)
      //Register all connections
      val results = Await.result(Future.traverse(names)(registerName), 10.seconds)
      unregisterOnTermination(results)
    } catch {
      case NonFatal(ex) ⇒
        shutdown()
        throw ex
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
  private def unregisterOnTermination(results: Seq[RegistrationResult]): Process = {
    log.info(results.map(_.location.connection.componentId).toString())

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "unregistering"
    )(() => unregisterServices(results))

    log.info(s"Executing specified command: ${command.commandText}")
    val process = command.commandText.run()
    Future(process.exitValue()).onComplete(_ ⇒ shutdown())
    process
  }

  /**
   * INTERNAL API : Unregisters a service.
   */
  private def unregisterServices(results: Seq[RegistrationResult]): Future[Done] = {
    log.info("Shutdown hook reached, unregistering services.")
    Future.traverse(results)(_.unregister()).map { _ =>
      log.info(s"Services are unregistered.")
      Done
    }
  }

  private def shutdown() = Await.result(cswCluster.shutdown(), 10.seconds)
}
