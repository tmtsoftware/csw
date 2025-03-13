/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.javadsl

import org.apache.pekko.actor.typed.javadsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl
import csw.alarm.client.internal.extensions.AlarmServiceExt.RichAlarmService
import csw.command.client.messages.TopLevelActorMessage
import csw.config.client.javadsl.JConfigClientFactory
import csw.event.client.internal.commons.EventServiceExt.RichEventService
import csw.framework.models.{CswContext, JCswContext}
import csw.framework.scaladsl.{ComponentHandlers, ComponentHandlersFactory}
import csw.location.client.extensions.LocationServiceExt.RichLocationService

/**
 * Base class for the factory for creating the behavior representing a component actor
 */
// The annotation is required to prevent a warning while interpreting a lambda into this SAM interface
@FunctionalInterface
abstract class JComponentHandlersFactory extends ComponentHandlersFactory() {

  def handlers(ctx: scaladsl.ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers = {
    import cswCtx.*
    import ctx.executionContext
    jHandlers(
      ctx.asJava,
      JCswContext(
        locationService.asJava,
        eventService.asJava,
        alarmService.asJava,
        timeServiceScheduler,
        loggerFactory.asJava,
        JConfigClientFactory.clientApi(ctx.system, locationService.asJava),
        currentStatePublisher,
        commandResponseManager,
        componentInfo
      )
    )
  }

  protected[framework] def jHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: JCswContext): JComponentHandlers
}
