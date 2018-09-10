package csw.services.location.agent

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import csw.messages.commons.CoordinatedShutdownReasons.{FailureReason, ProcessTerminatedReason}
import csw.messages.location.Connection.TcpConnection
import csw.services.location.api.models.TcpRegistration
import csw.messages.location.{ComponentId, ComponentType}
import csw.services.location.commons.CswCluster
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.location.agent.commons.LocationAgentLogger
import csw.services.location.agent.models.Command
import csw.services.logging.commons.LogAdminActorFactory
import csw.services.logging.scaladsl.Logger

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.control.NonFatal

/**
 * Starts a given external program ([[TcpConnection]]), registers it with the location service and unregisters it when the program exits.
 */
class LocationAgent(names: List[String], command: Command, actorSystem: ActorSystem) {
  private val log: Logger = LocationAgentLogger.getLogger

  private val cswCluster      = CswCluster.withSystem(actorSystem)
  private val locationService = LocationServiceFactory.withCluster(cswCluster)
  import cswCluster._

  // registers provided list of service names with location service
  // and starts a external program in child process using provided command
  def run(): Process =
    try {
      log.info(s"Executing specified command: ${command.commandText}")
      val process = command.commandText.run()
      // shutdown location agent on termination of external program started using provided command
      Future(process.exitValue()).onComplete(_ ⇒ shutdown(ProcessTerminatedReason))

      // delay the registration of component after executing the command
      Thread.sleep(command.delay)

      //Register all connections
      val results = Await.result(Future.traverse(names)(registerName), 10.seconds)
      unregisterOnTermination(results)

      process
    } catch {
      case NonFatal(ex) ⇒
        shutdown(FailureReason(ex))
        throw ex
    }

  // ================= INTERNAL API =================

  // Registers a single service as a TCP service
  private def registerName(name: String): Future[RegistrationResult] = {
    val componentId = ComponentId(name, ComponentType.Service)
    val connection  = TcpConnection(componentId)
    locationService.register(TcpRegistration(connection, command.port, LogAdminActorFactory.make(actorSystem)))
  }

  // Registers a shutdownHook to handle service un-registration during abnormal exit
  private def unregisterOnTermination(results: Seq[RegistrationResult]): Unit = {

    // Add task to unregister the TcpRegistration from location service
    // This task will get invoked before shutting down actor system
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "unregistering"
    )(() => unregisterServices(results))
  }

  private def unregisterServices(results: Seq[RegistrationResult]): Future[Done] = {
    log.info("Shutdown hook reached, un-registering connections", Map("services" → results.map(_.location.connection.name)))
    Future.traverse(results)(_.unregister()).map { _ =>
      log.info(s"Services are unregistered")
      Done
    }
  }

  private def shutdown(reason: Reason) = Await.result(cswCluster.shutdown(reason), 10.seconds)
}
