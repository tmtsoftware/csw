package csw.location.agent

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.http.scaladsl.Http
import csw.location.agent.commons.CoordinatedShutdownReasons.{FailureReason, ProcessTerminated}
import csw.location.agent.commons.LocationAgentLogger
import csw.location.agent.models.Command
import csw.location.agent.wiring.Wiring
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.logging.scaladsl.Logger

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.control.NonFatal

/**
 * Starts a given external program ([[TcpConnection]]), registers it with the location service and unregisters it when the program exits.
 */
class LocationAgent(names: List[String], command: Command, wiring: Wiring) {
  private val log: Logger = LocationAgentLogger.getLogger

  import wiring._
  import actorRuntime._

  // registers provided list of service names with location service
  // and starts a external program in child process using provided command
  def run(): Process =
    try {
      log.info(s"Executing specified command: ${command.commandText}")
      val process = command.commandText.run()
      // shutdown location agent on termination of external program started using provided command
      Future(process.exitValue()).onComplete(_ ⇒ shutdown(ProcessTerminated))

      // delay the registration of component after executing the command
      Thread.sleep(command.delay)

      //Register all connections as Http or Tcp
      val results = command.httpPath match {
        case Some(path) ⇒ Await.result(Future.traverse(names)(name ⇒ registerHttpName(name, path)), 10.seconds)
        case None       ⇒ Await.result(Future.traverse(names)(registerTcpName), 10.seconds)
      }

      unregisterOnTermination(results)

      process
    } catch {
      case NonFatal(ex) ⇒ shutdown(FailureReason(ex)); throw ex
    }

  // ================= INTERNAL API =================

  // Registers a single service as a TCP service
  private def registerTcpName(name: String): Future[RegistrationResult] = {
    val componentId = ComponentId(name, ComponentType.Service)
    val connection  = TcpConnection(componentId)
    locationService.register(TcpRegistration(connection, command.port))
  }

  // Registers a single service as a HTTP service
  private def registerHttpName(name: String, path: String): Future[RegistrationResult] = {
    val componentId = ComponentId(name, ComponentType.Service)
    val connection  = HttpConnection(componentId)
    locationService.register(HttpRegistration(connection, command.port, path))
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
      log.info("Services are unregistered")
      Done
    }
  }

  private def shutdown(reason: Reason) = Await.result(
    Http().shutdownAllConnectionPools().flatMap(_ => coordinatedShutdown.run(reason)),
    10.seconds
  )
}
