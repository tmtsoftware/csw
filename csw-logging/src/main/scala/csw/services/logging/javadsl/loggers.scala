package csw.services.logging.javadsl
import java.util.Optional

import akka.actor.{AbstractActor, ActorPath}
import akka.serialization.Serialization
import csw.services.logging.internal.JLogger
import csw.services.logging.scaladsl.LoggerImpl

import scala.compat.java8.OptionConverters.RichOptionalGeneric

trait JGenericLogger extends JBasicLogger {
  override protected def maybeComponentName: Optional[String] = Optional.empty()
}

abstract class JGenericLoggerActor extends JBasicLoggerActor {
  override protected def maybeComponentName: Optional[String] = Optional.empty()
}

trait JComponentLogger extends JBasicLogger {
  protected def componentName: String
  override protected def maybeComponentName: Optional[String] = Optional.of(componentName)
}

abstract class JComponentLoggerActor extends JBasicLoggerActor {
  protected def componentName: String
  override protected def maybeComponentName: Optional[String] = Optional.of(componentName)
}

trait JBasicLogger {
  protected def maybeComponentName: Optional[String]
  protected def getLogger: ILogger = {
    val log = new LoggerImpl(maybeComponentName.asScala, None)
    new JLogger(log, getClass)
  }
}

abstract class JBasicLoggerActor extends AbstractActor {
  protected def maybeComponentName: Optional[String]
  protected def getLogger: ILogger = {
    val actorName = ActorPath.fromString(Serialization.serializedActorPath(getSelf)).toString
    val log       = new LoggerImpl(maybeComponentName.asScala, Some(actorName))
    new JLogger(log, getClass)
  }
}
