package csw.services.location.internal

import akka.Done
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata._
import akka.pattern.ask
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.services.location.internal.StreamExt.RichSource
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationService}

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

private[location] class LocationServiceImpl(actorRuntime: ActorRuntime) extends LocationService { outer =>

  import actorRuntime._

  def register(location: Location): Future[RegistrationResult] = {
    val key = LWWRegisterKey[Option[Location]](location.connection.name)
    val updateValue = Update(key, LWWRegister(Option.empty[Location]), WriteMajority(5.seconds)) {
      case LWWRegister(Some(loc)) if loc != location => throw new IllegalStateException(
        s"there is another location=$loc registered against name=${location.connection.name}."
      )
      case r@LWWRegister(_)                          => r.withValue(Some(location))
    }

    val updateRegistry = Update(Constants.RegistryKey, LWWMap.empty[Connection, Location], WriteMajority(5.seconds))(_ + (location.connection → location))

    (replicator ? updateValue).flatMap {
      case _: UpdateSuccess[_]               => (replicator ? updateRegistry).map {
        case _: UpdateSuccess[_] => registrationResult(location.connection)
        case _                   => throw new RuntimeException(s"unable to register ${location.connection}")
      }
      case ModifyFailure(`key`, _, cause, _) => throw cause
      case _                                 => Future.failed(new RuntimeException(s"unable to register ${location.connection}"))
    }
  }

  def unregister(connection: Connection): Future[Done] = {
    val key = LWWRegisterKey[Option[Location]](connection.name)
    val updateValue = Update(key, LWWRegister(Option.empty[Location]), WriteMajority(5.seconds))(_.withValue(None))
    val deleteFromRegistry = Update(Constants.RegistryKey, LWWMap.empty[Connection, Location], WriteMajority(5.seconds))(_ - connection)
    (replicator ? updateValue).flatMap {
      case x: UpdateSuccess[_] => (replicator ? deleteFromRegistry).map {
        case _: UpdateSuccess[_] => Done
        case _                   => throw new RuntimeException(s"unable to unregister $connection")
      }
      case ModifyFailure(`key`, _, cause, _) => throw cause
      case _                                 => Future.failed(new RuntimeException(s"unable to unregister $connection"))
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

  def list: Future[List[Location]] = {
    val get = Get(Constants.RegistryKey, ReadMajority(5.seconds))
    (replicator ? get).map {
      case x@GetSuccess(Constants.RegistryKey, _) => x.get(Constants.RegistryKey).entries.values.toList
      case NotFound(Constants.RegistryKey, _)     ⇒ List.empty
      case _                                      => throw new RuntimeException(s"unable to get the list of registered locations")
    }
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
    val key = LWWRegisterKey[Option[Location]](connection.name)
    actorRefF.foreach(actorRef ⇒ replicator ! Subscribe(key, actorRef))
    source.collect {
      case c@Changed(`key`) if c.get(key).value.isDefined => LocationUpdated(c.get(key).value.get)
      case c@Changed(`key`)                               => LocationRemoved(connection)
    }.cancellable
  }

  private def registrationResult(connection: Connection): RegistrationResult = new RegistrationResult {
    override def componentId: ComponentId = connection.componentId

    override def unregister(): Future[Done] = outer.unregister(connection)
  }

}
