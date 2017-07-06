package csw.services.logging.internal

import java.util.function.Supplier

import csw.services.logging.javadsl.ILogger
import csw.services.logging.macros.SourceFactory
import csw.services.logging.scaladsl.{noId, AnyId, Logger}

import scala.collection.JavaConverters.mapAsScalaMapConverter

class JLogger private[logging] (log: Logger, cls: Class[_]) extends ILogger {

  override def trace(msg: Supplier[String]): Unit                = log.trace(msg.get)(SourceFactory.from(cls))
  override def trace(msg: Supplier[String], ex: Throwable): Unit = log.trace(msg.get, ex = ex)(SourceFactory.from(cls))
  override def trace(msg: Supplier[String], id: AnyId): Unit     = log.trace(msg.get, id = id)(SourceFactory.from(cls))
  override def trace(msg: Supplier[String], ex: Throwable, id: AnyId): Unit =
    log.trace(msg.get, ex = ex, id = id)(SourceFactory.from(cls))
  override def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit =
    log.trace(msg.get, map.get.asScala.toMap)(SourceFactory.from(cls))
  override def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit =
    log.trace(msg.get, map.get.asScala.toMap, ex)(SourceFactory.from(cls))
  override def trace(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit =
    log.trace(msg.get, map.get.asScala.toMap, id = id)
  override def trace(msg: Supplier[String],
                     map: Supplier[java.util.Map[String, Object]],
                     ex: Throwable,
                     id: AnyId): Unit =
    log.trace(msg.get, map.get.asScala.toMap, ex, id)(SourceFactory.from(cls))

  override def debug(msg: Supplier[String]): Unit                = log.debug(msg.get)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String], ex: Throwable): Unit = log.debug(msg.get, ex = ex)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String], id: AnyId): Unit     = log.debug(msg.get, id = id)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String], ex: Throwable, id: AnyId): Unit =
    log.debug(msg.get, ex = ex, id = id)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit =
    log.debug(msg.get, map.get.asScala.toMap)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit =
    log.debug(msg.get, map.get.asScala.toMap, ex = ex)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit =
    log.debug(msg.get, map.get.asScala.toMap, id = id)(SourceFactory.from(cls))
  override def debug(msg: Supplier[String],
                     map: Supplier[java.util.Map[String, Object]],
                     ex: Throwable,
                     id: AnyId = noId): Unit =
    log.debug(msg.get, map.get.asScala.toMap, ex, id)(SourceFactory.from(cls))

  override def info(msg: Supplier[String]): Unit                = log.info(msg.get)(SourceFactory.from(cls))
  override def info(msg: Supplier[String], ex: Throwable): Unit = log.info(msg.get, ex = ex)(SourceFactory.from(cls))
  override def info(msg: Supplier[String], id: AnyId): Unit     = log.info(msg.get, id = id)(SourceFactory.from(cls))
  override def info(msg: Supplier[String], ex: Throwable, id: AnyId): Unit =
    log.info(msg.get, ex = ex, id = id)(SourceFactory.from(cls))
  override def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit =
    log.info(msg.get, map.get.asScala.toMap)(SourceFactory.from(cls))
  override def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit =
    log.info(msg.get, map.get.asScala.toMap, ex = ex)(SourceFactory.from(cls))
  override def info(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit =
    log.info(msg.get, map.get.asScala.toMap, id = id)(SourceFactory.from(cls))
  override def info(msg: Supplier[String],
                    map: Supplier[java.util.Map[String, Object]],
                    ex: Throwable,
                    id: AnyId): Unit =
    log.info(msg.get, map.get.asScala.toMap, ex, id)(SourceFactory.from(cls))

  override def warn(msg: Supplier[String]): Unit                = log.warn(msg.get)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String], ex: Throwable): Unit = log.warn(msg.get, ex = ex)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String], id: AnyId): Unit     = log.warn(msg.get, id = id)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String], ex: Throwable, id: AnyId): Unit =
    log.warn(msg.get, ex = ex, id = id)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit =
    log.warn(msg.get, map.get.asScala.toMap)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit =
    log.warn(msg.get, map.get.asScala.toMap, ex = ex)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit =
    log.warn(msg.get, map.get.asScala.toMap, id = id)(SourceFactory.from(cls))
  override def warn(msg: Supplier[String],
                    map: Supplier[java.util.Map[String, Object]],
                    ex: Throwable,
                    id: AnyId): Unit =
    log.warn(msg.get, map.get.asScala.toMap, ex, id)(SourceFactory.from(cls))

  override def error(msg: Supplier[String]): Unit                = log.error(msg.get)(SourceFactory.from(cls))
  override def error(msg: Supplier[String], ex: Throwable): Unit = log.error(msg.get, ex = ex)(SourceFactory.from(cls))
  override def error(msg: Supplier[String], id: AnyId): Unit     = log.error(msg.get, id = id)(SourceFactory.from(cls))
  override def error(msg: Supplier[String], ex: Throwable, id: AnyId): Unit =
    log.error(msg.get, ex = ex, id = id)(SourceFactory.from(cls))
  override def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit =
    log.error(msg.get, map.get.asScala.toMap)(SourceFactory.from(cls))
  override def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit =
    log.error(msg.get, map.get.asScala.toMap, ex = ex)(SourceFactory.from(cls))
  override def error(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit =
    log.error(msg.get, map.get.asScala.toMap, id = id)(SourceFactory.from(cls))
  override def error(msg: Supplier[String],
                     map: Supplier[java.util.Map[String, Object]],
                     ex: Throwable,
                     id: AnyId): Unit =
    log.error(msg.get, map.get.asScala.toMap, ex, id)(SourceFactory.from(cls))

  override def fatal(msg: Supplier[String]): Unit                = log.fatal(msg.get)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String], ex: Throwable): Unit = log.fatal(msg.get, ex = ex)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String], id: AnyId): Unit     = log.fatal(msg.get, id = id)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String], ex: Throwable, id: AnyId): Unit =
    log.fatal(msg.get, ex = ex, id = id)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]]): Unit =
    log.fatal(msg.get, map.get.asScala.toMap)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], ex: Throwable): Unit =
    log.fatal(msg.get, map.get.asScala.toMap, ex = ex)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String], map: Supplier[java.util.Map[String, Object]], id: AnyId): Unit =
    log.fatal(msg.get, map.get.asScala.toMap, id = id)(SourceFactory.from(cls))
  override def fatal(msg: Supplier[String],
                     map: Supplier[java.util.Map[String, Object]],
                     ex: Throwable,
                     id: AnyId): Unit =
    log.fatal(msg.get, map.get.asScala.toMap, ex, id)(SourceFactory.from(cls))

  override def asScala: Logger = log
}
