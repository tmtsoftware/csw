package csw.services.logging.internal

import java.util.Optional
import java.util.function.Supplier

import csw.services.logging._
import csw.services.logging.javadsl.ILogger
import csw.services.logging.macros.SourceFactory
import csw.services.logging.scaladsl.{noId, AnyId, LoggerImpl}

import scala.compat.java8.OptionConverters.RichOptionalGeneric

class JLogger private[logging] (componentName: Optional[String], actorName: Optional[String], cls: Class[_])
    extends ILogger {

  val log = new LoggerImpl(componentName.asScala, actorName.asScala)

  override def info(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit =
    log.info(msg.get(), ex, id)(SourceFactory.from(cls))

  override def info(msg: Supplier[Object], id: AnyId): Unit = info(msg, noException, id)

  override def info(msg: Supplier[Object], ex: Throwable): Unit = info(msg, ex, noId)

  override def info(msg: Supplier[Object]): Unit = info(msg, noException, noId)
}
