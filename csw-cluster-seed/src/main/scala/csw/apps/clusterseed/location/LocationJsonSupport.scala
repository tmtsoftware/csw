package csw.apps.clusterseed.location
import java.net.URI

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.serialization.{Serialization, SerializationExtension}
import csw.messages.location.Connection.AkkaConnection
import csw.services.location.models.AkkaRegistration
import csw.services.logging.messages.LogControlMessages
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

trait LocationJsonSupport {
  implicit def actorRefDecoder[T](implicit actorSystem: ActorSystem): Decoder[ActorRef[T]] = {
    val provider = SerializationExtension(actorSystem).system.provider
    Decoder.decodeString.map(path => provider.resolveActorRef(path))
  }

  implicit def actorRefEncoder[T]: Encoder[ActorRef[T]] = {
    Encoder.encodeString.contramap(actoRef => Serialization.serializedActorPath(actoRef.toUntyped))
  }

  implicit val uriDecoder: Decoder[URI] = Decoder.decodeString.map(path => new URI(path))
  implicit val uriEncoder: Encoder[URI] = Encoder.encodeString.contramap(uri => uri.toString)

  implicit def akkaRegistrationDecoder(implicit actorSystem: ActorSystem): Decoder[AkkaRegistration] = { cursor =>
    for {
      connection       <- cursor.downField("connection").as[AkkaConnection]
      prefix           <- cursor.downField("prefix").as[Option[String]]
      actorRef         <- cursor.downField("actorRef").as[ActorRef[Any]]
      logAdminActorRef <- cursor.downField("logAdminActorRef").as[ActorRef[LogControlMessages]]
    } yield {
      AkkaRegistration(connection, prefix, actorRef, logAdminActorRef)
    }
  }

  implicit val akkaRegistrationEncoder: Encoder[AkkaRegistration] = { akkaRegistration =>
    Json.obj(
      ("connection", akkaRegistration.connection.asJson),
      ("prefix", akkaRegistration.prefix.asJson),
      ("actorRef", akkaRegistration.actorRef.asJson),
      ("logAdminActorRef", akkaRegistration.logAdminActorRef.asJson)
    )
  }
}
