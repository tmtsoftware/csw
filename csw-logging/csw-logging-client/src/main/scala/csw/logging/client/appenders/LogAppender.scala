/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.appenders

import org.apache.pekko.actor.typed.ActorSystem
import play.api.libs.json.JsObject

import scala.concurrent.Future

/**
 * Trait for log appender companion objects
 */
trait LogAppenderBuilder {

  /**
   * Log appender constructor
   *
   * @param system typed Actor System
   * @param standardHeaders the headers that are fixes for this service
   * @return an appender
   */
  def apply(system: ActorSystem[?], standardHeaders: JsObject): LogAppender
}

/**
 * Trait for log appender classes.
 */
trait LogAppender {

  /**
   * Appends a new log message
   *
   * @param baseMsg the message to be logged
   * @param category the kinds of log (for example, "common")
   */
  def append(baseMsg: JsObject, category: String): Unit

  /**
   * Called just before the logger shuts down
   *
   * @return a future that is completed when finished
   */
  def finish(): Future[Unit]

  /**
   * Stops a log appender
   *
   * @return a future that is completed when stopped
   */
  def stop(): Future[Unit]

}
