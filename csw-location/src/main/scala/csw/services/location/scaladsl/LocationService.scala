package csw.services.location.scaladsl

import java.net.{InetAddress, NetworkInterface}
import javax.jmdns.JmDNS

import akka.actor.ActorSystem
import akka.stream.KillSwitch
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import csw.services.location.common.Networks
import csw.services.location.models._

import scala.concurrent.Future

trait LocationService {

  def register(reg: TcpRegistration): Future[RegistrationResult]
  def register(reg: HttpRegistration): Future[RegistrationResult]
  def register(reg: AkkaRegistration): Future[RegistrationResult]

  /**
    * Unregisters the connection from the location service
    * (Note: it can take some time before the service is removed from the list: see
    * comments in registry.unregisterService())
    */
  def unregister(connection: Connection): Future[Done]

  /**
    * Convenience method that gets the location service information for a given set of services.
    *
    * @param connections set of requested connections
    * @return a future object describing the services found
    */
  def resolve(connections: Set[Connection]): Future[Set[Location]]

  def list: Future[List[Location]]

  def list(componentType: ComponentType): Future[List[Location]]

  def list(hostname: String): Future[List[Location]]

  def list(connectionType: ConnectionType): Future[List[Location]]

  def track(connection: Connection): Source[Location, KillSwitch]
}

object LocationService {
  val DnsType = "_csw._tcp.local."
  val PathKey = "path"

  private val jmDNS: JmDNS = JmDNS.create(NetworkInterface.getByName("eth0").getInetAddresses().nextElement());
  private val actorSystem = ActorSystem("location-service")

  def make(): LocationService = make(jmDNS, actorSystem)

  private[scaladsl] def make(jmDNS: JmDNS, actorSystem: ActorSystem): LocationService = {
    new LocationServiceImpl(jmDNS, actorSystem)
  }
}
