package csw.location.agent

import akka.Done
import akka.actor.CoordinatedShutdown
import csw.location.agent.commons.LocationAgentLogger
import csw.location.agent.models.Command
import csw.location.agent.wiring.Wiring
import csw.location.api.models
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.location.api.scaladsl.RegistrationResult
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix

import scala.collection.immutable.Seq
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/**
 * Starts a given external program ([[Connection.TcpConnection]]), registers it with the location service and unregisters it when the program exits.
 */
class LocationAgent(prefixes: List[Prefix], command: Command, networkType: NetworkType, wiring: Wiring) {
  private val log: Logger = LocationAgentLogger.getLogger

  private val timeout: FiniteDuration = 10.seconds
  import wiring._
  import actorRuntime._

  // registers provided list of service names with location service
  // and starts a external program in child process using provided command
  def run(): Process =
    try {
      log.info(s"Executing specified command: ${command.commandText}")
      val process = Runtime.getRuntime.exec(command.commandText)
      // shutdown location agent on termination of external program started using provided command
      process.onExit().toScala.onComplete(_ => shutdown())

      // delay the registration of component after executing the command
      Thread.sleep(command.delay)

      //Register all connections as Http or Tcp
      val results = command.httpPath match {
        case Some(path) => Await.result(Future.traverse(prefixes)(registerHttpName(_, path)), timeout)
        case None       => Await.result(Future.traverse(prefixes)(registerTcpName), timeout)
      }

      unregisterOnTermination(results)

      process
    }
    catch {
      case NonFatal(ex) => shutdown(); throw ex
    }

  // ================= INTERNAL API =================

  // Registers a single service as a TCP service
  private def registerTcpName(prefix: Prefix): Future[RegistrationResult] = {
    val componentId = ComponentId(prefix, ComponentType.Service)
    val connection  = TcpConnection(componentId)
    locationService.register(TcpRegistration(connection, command.port))
  }

  // Registers a single service as a HTTP service with provided path
  private def registerHttpName(prefix: Prefix, path: String): Future[RegistrationResult] = {
    val componentId = models.ComponentId(prefix, ComponentType.Service)
    val connection  = HttpConnection(componentId)
    locationService.register(HttpRegistration(connection, command.port, path, networkType))
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
    log.info("Shutdown hook reached, un-registering connections", Map("services" -> results.map(_.location.connection.name)))
    Future.traverse(results)(_.unregister()).map { _ =>
      log.info("Services are unregistered")
      Done
    }
  }

  private def shutdown() = Await.result(actorRuntime.shutdown(), timeout)
}
