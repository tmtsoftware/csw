package csw.services.internal

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http.ServerBinding
import csw.config.server.http.HttpService
import csw.config.server.{ServerWiring => ConfigWiring}
import csw.location.agent.wiring.{Wiring => AgentWiring}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.internal.{ServerWiring => LocationWiring}
import csw.services._
import csw.services.cli.Command.Start
import csw.services.utils.ColoredConsole
import org.tmt.embedded_keycloak.impl.StopHandle

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.NonFatal

class Wiring(startCmd: Start) {
  import startCmd._
  lazy implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  lazy implicit val ec: ExecutionContext                            = actorSystem.executionContext

  lazy val settings: Settings               = Settings(startCmd.interfaceName)
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  lazy val environment   = new Environment(settings)
  lazy val locationAgent = new LocationAgent(settings)
  lazy val redis         = new Redis(settings)
  lazy val keycloak      = new AuthServer(locationService, settings)

  lazy val lazyLocationService: Option[(ServerBinding, LocationWiring)] = LocationServer.start(settings.clusterPort)
  lazy val lazyEventProcess: Option[Process]                            = start(event, redis.startEvent())
  lazy val lazyAlarmProcess: Option[Process]                            = start(alarm, redis.startAlarm())
  lazy val lazySentinel: Option[(Process, AgentWiring)] =
    start(event || alarm, locationAgent.startSentinel(event, alarm)).flatten

  lazy val lazyDatabaseService: Option[(Process, AgentWiring)] = start(database, locationAgent.startPostgres()).flatten
  lazy val lazyKeycloak: Option[StopHandle]                    = start(config || auth, keycloak.start())

  lazy val lazyConfigService: Option[(HttpService, ConfigWiring)] =
    start(config, ConfigServer.start(settings.configPort)).flatten

  def shutdown(): Unit = {
    ColoredConsole.GREEN.println("Shutdown started ...")
    lazyConfigService.foreach {
      case (_, wiring) => block(wiring.actorRuntime.shutdown())
    }
    Future(lazyKeycloak.foreach(_.stop())) // fixme: there is bug in Keycloak.stop, for now, let it run/fail in the background
    killAgent(lazyDatabaseService)
    kill(lazyEventProcess, lazyAlarmProcess)
    killAgent(lazySentinel)
    lazyLocationService.foreach {
      case (_, wiring) => block(wiring.actorRuntime.shutdown())
    }
    ColoredConsole.GREEN.println("Shutdown finished!")
  }

  private def kill(process: Option[Process]*): Unit = process.foreach(_.map(_.destroyForcibly()))
  private def killAgent(agent: Option[(Process, AgentWiring)]): Unit = agent.foreach {
    case (process, wiring) => process.destroyForcibly(); block(wiring.actorRuntime.shutdown())
  }
  private def block[T](thunk: => Future[T], duration: FiniteDuration = 5.seconds): T = Await.result(thunk, duration)
  private def start[T](flag: Boolean, service: => T): Option[T]                      = if (flag) ignoreException(service) else None

  private def ignoreException[T](thunk: => T): Option[T] =
    try Some(thunk)
    catch {
      case NonFatal(_) => None
    }
}
