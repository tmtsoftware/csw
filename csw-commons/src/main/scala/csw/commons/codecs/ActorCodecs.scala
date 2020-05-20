package csw.commons.codecs

import akka.actor.typed.{ActorRef, ActorRefResolver, ActorSystem}
import io.bullet.borer.Codec

trait ActorCodecs {
  implicit def actorSystem: ActorSystem[_]

  implicit def actorRefCodec[T]: Codec[ActorRef[T]] = {
    val resolver = ActorRefResolver(actorSystem)

    Codec.bimap[String, ActorRef[T]](
      resolver.toSerializationFormat,
      resolver.resolveActorRef
    )
  }
}
