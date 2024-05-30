/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.logging.client.javadsl

import org.apache.pekko.actor
import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.adapter.TypedActorRefOps
import org.apache.pekko.actor.{ActorPath, ActorRef, typed}
import org.apache.pekko.serialization.Serialization
import csw.logging.api.javadsl.ILogger
import csw.logging.api.scaladsl.Logger
import csw.logging.client.internal.{JLoggerImpl, LoggerImpl}
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix

abstract class JBaseLoggerFactory private[logging] (maybePrefix: Option[Prefix]) {
  def getLogger[T](ctx: ActorContext[T], klass: Class[?]): ILogger = new JLoggerImpl(logger(Some(actorPath(ctx.getSelf))), klass)
  def getLogger(ctx: actor.ActorContext, klass: Class[?]): ILogger = new JLoggerImpl(logger(Some(actorPath(ctx.self))), klass)
  def getLogger(klass: Class[?]): ILogger                          = new JLoggerImpl(logger(None), klass)

  private def logger(maybeActorRef: Option[String]): Logger = new LoggerImpl(maybePrefix, maybeActorRef)
  private def actorPath(actorRef: ActorRef): String = ActorPath.fromString(Serialization.serializedActorPath(actorRef)).toString
  private def actorPath(actorRef: typed.ActorRef[?]): String = actorPath(actorRef.toClassic)
}

/**
 * When using the `JLoggerFactory`, log statements will have `@componentName` tag with provided `prefix`
 *
 * @param prefix to appear in log statements
 */
class JLoggerFactory(prefix: Prefix) extends JBaseLoggerFactory(Some(prefix)) {

  /**
   * Returns the scala API for this instance of JLoggerFactory
   */
  def asScala: LoggerFactory = new LoggerFactory(prefix)
}

/**
 * When using the `JGenericLoggerFactory`, log statements will not have `@componentName` tag
 */
object JGenericLoggerFactory extends JBaseLoggerFactory(None)
