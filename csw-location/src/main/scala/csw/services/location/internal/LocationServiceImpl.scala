package csw.services.location.internal

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.internal.Registry.AllServices
import csw.services.location.internal.StreamExt.RichSource
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationService}

import scala.async.Async._
import scala.concurrent.Future

private[location] class LocationServiceImpl(actorRuntime: ActorRuntime) extends LocationService { outer =>

  import actorRuntime._

  def register(registration: Registration): Future[RegistrationResult] = {
    val location = registration.location(Networks.hostname())

    val service = new Registry.Service(location.connection)

    val updateValue = service.update {
      case r@LWWRegister(Some(`location`) | None) => r.withValue(Some(location))
      case LWWRegister(Some(otherLocation))       => otherLocationRegisteredError(location, otherLocation)
    }

    val updateRegistry = AllServices.update(_ + (location.connection → location))

    (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_]                     => (replicator ? updateRegistry).map {
        case _: UpdateSuccess[_] => registrationResult(location.connection)
        case _                   => registrationFailedError(location.connection)
      }
      case ModifyFailure(service.Key, _, cause, _) => throw cause
      case _                                       => registrationFailedError(location.connection)
    }
  }

  def unregister(connection: Connection): Future[Done] = {
    val service = new Registry.Service(connection)

    (replicator ? service.update(_.withValue(None))).flatMap {
      case x: UpdateSuccess[_] => (replicator ? AllServices.update(_ - connection)).map {
        case _: UpdateSuccess[_] => Done
        case _                   => unregistrationFailedError(connection)
      }
      case _                   => unregistrationFailedError(connection)
    }
  }

  def unregisterAll(): Future[Done] = async {
    val locations = await(list)
    await(Future.traverse(locations)(loc ⇒ unregister(loc.connection)))
    Done
  }

  def resolve(connection: Connection): Future[Option[Location]] = async {
    await(list).find(_.connection == connection)
  }

  def list: Future[List[Location]] = (replicator ? AllServices.get).map {
    case x@GetSuccess(AllServices.Key, _) => x.get(AllServices.Key).entries.values.toList
    case NotFound(AllServices.Key, _)     ⇒ List.empty
    case _                                => listingError()
  }

  def list(componentType: ComponentType): Future[List[Location]] = async {
    await(list).filter(_.connection.componentId.componentType == componentType)
  }

  def list(hostname: String): Future[List[Location]] = async {
    await(list).filter(_.uri.getHost == hostname)
  }

  def list(connectionType: ConnectionType): Future[List[Location]] = async {
    await(list).filter(_.connection.connectionType == connectionType)
  }

  def track(connection: Connection): Source[TrackingEvent, KillSwitch] = {
    val (source, actorRefF) = StreamExt.actorCoupling[Any]
    val service = new Registry.Service(connection)
    actorRefF.foreach(actorRef ⇒ replicator ! Subscribe(service.Key, actorRef))
    source.collect {
      case c@Changed(service.Key) if c.get(service.Key).value.isDefined => LocationUpdated(c.get(service.Key).value.get)
      case c@Changed(service.Key)                                       => LocationRemoved(connection)
    }.cancellable
  }

  private def registrationResult(connection: Connection): RegistrationResult = new RegistrationResult {
    override def componentId: ComponentId = connection.componentId

    override def unregister(): Future[Done] = outer.unregister(connection)
  }

  private def registrationFailedError(connection: Connection) = throw new RuntimeException(s"unable to register $connection")

  private def unregistrationFailedError(connection: Connection) = throw new RuntimeException(s"unable to unregister $connection")

  private def otherLocationRegisteredError(location: Location, otherLocation: Location) = throw new IllegalStateException(
    s"there is other location=$otherLocation registered against name=${location.connection.name}."
  )

  private def listingError() = throw new RuntimeException(s"unable to get the list of registered locations")

}
