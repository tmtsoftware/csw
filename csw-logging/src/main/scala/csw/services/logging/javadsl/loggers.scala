package csw.services.logging.javadsl
import java.util.Optional

import akka.actor.{AbstractActor, ActorPath}
import akka.serialization.Serialization
import csw.services.logging.internal.JLogger

trait JComponentLogger {
  def componentName: Optional[String]
  def getLogger: JLogger = new JLogger(componentName, Optional.empty(), getClass)
}

abstract class JComponentLoggerActor extends AbstractActor {
  def componentName: Optional[String]
  protected def getLogger: JLogger = {
    val actorName = ActorPath.fromString(Serialization.serializedActorPath(getSelf)).toString
    new JLogger(componentName, Optional.of(actorName), getClass)
  }
}

trait JGenericLogger extends JComponentLogger {
  override def componentName: Optional[String] = Optional.empty()
}

abstract class JGenericLoggerActor extends JComponentLoggerActor {
  override def componentName: Optional[String] = Optional.empty()
}
