package csw.location.client.javadsl

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.location.api.javadsl.ILocationService
import csw.location.client.extensions.LocationServiceExt.RichLocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory

/**
 * The factory is used to create ILocationService instance.
 */
object JHttpLocationServiceFactory {

  /**
   * Use this factory method to create http location client when location server is running locally.
   * HTTP Location server runs on port 7654.
   * */
  def makeLocalClient(actorSystem: ActorSystem, mat: Materializer): ILocationService =
    HttpLocationServiceFactory.makeLocalClient(actorSystem, mat).asJava(actorSystem.dispatcher)

}
