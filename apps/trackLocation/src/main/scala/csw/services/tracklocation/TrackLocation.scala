package csw.services.tracklocation

import java.util.concurrent.atomic.AtomicBoolean

import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import csw.services.location.common.ActorRuntime
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, RegistrationResult, TcpRegistration}
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.tracklocation.models.Command

import scala.collection.immutable.Seq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}
import sys.process._

class TrackLocation(names: List[String], command: Command, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private var isRunning = new AtomicBoolean(true)

  private implicit val timeout = Timeout(10.seconds)
  private val locationService = LocationServiceFactory.make(actorRuntime)

  def run(): Future[Unit] = register().map(awaitTermination)

  private def register(): Future[Seq[RegistrationResult]] = Source(names)
    .initialDelay(command.delay.millis) //delay to give the app a chance to start
    .mapAsync(1)(registerName)
    .runWith(Sink.seq)

  private def registerName(name: String): Future[RegistrationResult] = {
    val componentId = ComponentId(name, ComponentType.Service)
    val connection = TcpConnection(componentId)
    locationService.register(TcpRegistration(connection, command.port))
  }


  private def awaitTermination(results: Seq[RegistrationResult]): Unit = {
    println(results.map(_.componentId))

    sys.addShutdownHook {
      println("Shutdown hook reached, unregistering services.")
      unregisterServices(results)
      println(s"Exited the application.")
    }

    isRunning = true
    println(s"Executing specified command: ${command.commandText}")
    val exitCode = command.commandText.!
    println(s"$command exited with exit code $exitCode")

    unregisterServices(results)
    if (!command.noExit) System.exit(exitCode)
  }

  private def unregisterServices(results: Seq[RegistrationResult]): Unit = synchronized {
    if(isRunning) {
      Await.result(Future.traverse(results)(_.unregister()), 10.seconds)
      isRunning = false
      println(s"Services are unregistered.")
    }
  }
}
