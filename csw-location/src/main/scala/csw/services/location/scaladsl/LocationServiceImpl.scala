package csw.services.location.scaladsl

import javax.jmdns.{JmDNS, ServiceInfo}

import akka.Done
import akka.actor.ActorSystem
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.models._

import scala.concurrent.Future

private class LocationServiceImpl(jmDNS: JmDNS, actorSystem: ActorSystem) extends LocationService { outer =>

  private val jmDnsDispatcher = actorSystem.dispatchers.lookup("jmdns.dispatcher")

  override def register(reg: TcpRegistration): Future[RegistrationResult] = Future {
    jmDNS.registerService(
      ServiceInfo.create(LocationService.DnsType, reg.connection.toString, reg.port, "")
    )
    new RegistrationResult {
      override def componentId: ComponentId = reg.connection.componentId

      override def unregister(): Future[Done] = outer.unregister(reg.connection)
    }
  }(jmDnsDispatcher)

  override def register(reg: AkkaRegistration): Future[RegistrationResult] = ???

  override def register(reg: HttpRegistration): Future[RegistrationResult] = ???

  override def unregister(connection: Connection): Future[Done] = ???

  override def resolve(connections: Set[Connection]): Future[Set[Location]] = ???

  override def list: Future[List[Location]] = Future {
    jmDNS.list(LocationService.DnsType).toList.flatMap(Location.fromServiceInfo)
  }(jmDnsDispatcher)

  override def list(componentType: ComponentType): Future[List[Location]] = ???

  override def list(hostname: String): Future[List[Location]] = ???

  override def list(connectionType: ConnectionType): Future[List[Location]] = ???

  override def track(connection: Connection): Source[Location, KillSwitch] = ???

}