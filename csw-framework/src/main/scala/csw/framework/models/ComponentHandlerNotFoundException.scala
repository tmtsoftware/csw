package csw.framework.models

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage

class ComponentHandlerNotFoundException extends RuntimeException {
  override def getMessage: String = s"Constructor for creating component handler does not exist." +
    s" Expected a Scala class having a constructor declared with parameters types (${classOf[ActorContext[TopLevelActorMessage]]}, ${classOf[CswContext]}) or " +
    s"a Java class having a constructor declared with parameter types (${classOf[akka.actor.typed.javadsl.ActorContext[TopLevelActorMessage]]}, ${classOf[JCswContext]})"
}
