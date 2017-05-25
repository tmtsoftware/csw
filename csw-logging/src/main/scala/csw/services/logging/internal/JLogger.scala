package csw.services.logging.internal

import java.util.function.Supplier

import csw.services.logging._
import csw.services.logging.javadsl.ILogger
import csw.services.logging.macros.SourceFactory
import csw.services.logging.scaladsl.{noId, AnyId, Logger}

import scala.collection.JavaConverters.mapAsScalaMapConverter

class JLogger private[logging] (log: Logger, cls: Class[_]) extends ILogger {

  override def trace(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit =
    log.trace(msg.get(), ex, id)(SourceFactory.from(cls))
  override def trace(msg: Supplier[Object], id: AnyId): Unit     = trace(msg, noException, id)
  override def trace(msg: Supplier[Object], ex: Throwable): Unit = trace(msg, ex, noId)
  override def trace(msg: Supplier[Object]): Unit                = trace(msg, noException, noId)

  override def debug(msg: Supplier[Object], ex: Throwable, id: AnyId = noId): Unit =
    log.debug(msg.get(), ex, id)(SourceFactory.from(cls))
  override def debug(msg: Supplier[Object], id: AnyId): Unit     = debug(msg, noException, id)
  override def debug(msg: Supplier[Object], ex: Throwable): Unit = debug(msg, ex, noId)
  override def debug(msg: Supplier[Object]): Unit                = debug(msg, noException, noId)

  override def info(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit =
    log.info(msg.get(), ex, id)(SourceFactory.from(cls))
  override def info(msg: Supplier[Object], id: AnyId): Unit     = info(msg, noException, id)
  override def info(msg: Supplier[Object], ex: Throwable): Unit = info(msg, ex, noId)
  override def info(msg: Supplier[Object]): Unit                = info(msg, noException, noId)

  override def warn(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit =
    log.warn(msg.get(), ex, id)(SourceFactory.from(cls))
  override def warn(msg: Supplier[Object], id: AnyId): Unit     = debug(msg)
  override def warn(msg: Supplier[Object], ex: Throwable): Unit = warn(msg, ex, noId)
  override def warn(msg: Supplier[Object]): Unit                = warn(msg, noException, noId)

  override def error(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit =
    log.error(msg.get(), ex, id)(SourceFactory.from(cls))
  override def error(msg: Supplier[Object], id: AnyId): Unit     = error(msg, noException, id)
  override def error(msg: Supplier[Object], ex: Throwable): Unit = error(msg, ex, noId)
  override def error(msg: Supplier[Object]): Unit                = error(msg, noException, noId)

  override def fatal(msg: Supplier[Object], ex: Throwable, id: AnyId): Unit =
    log.fatal(msg.get(), ex, id)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[Object], id: AnyId): Unit     = fatal(msg, noException, id)
  override def fatal(msg: Supplier[Object], ex: Throwable): Unit = fatal(msg, ex, noId)
  override def fatal(msg: Supplier[Object]): Unit                = fatal(msg, noException, noId)

  override def alternative(category: String, msg: java.util.Map[String, Object], ex: Throwable, id: AnyId): Unit =
    log.alternative(category, msg.asScala.toMap, ex)
  override def alternative(category: String, msg: java.util.Map[String, Object], id: AnyId): Unit =
    alternative(category, msg, noException, id)
  override def alternative(category: String, msg: java.util.Map[String, Object], ex: Throwable): Unit =
    alternative(category, msg, ex, noId)
  override def alternative(category: String, msg: java.util.Map[String, Object]): Unit =
    alternative(category, msg, noException, noId)

  override def asScala: Logger = log
}
