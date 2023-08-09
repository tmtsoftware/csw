/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.models.framework

import org.apache.pekko.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.params.commands.Nameable
import csw.params.core.states.StateName
import csw.serializable.CommandSerializable

/**
 * LifecycleStateChanged represents a notification of state change in a component
 *
 * @param publisher the reference of component's supervisor for which the state changed
 * @param state the new state the component went into
 */
case class LifecycleStateChanged(publisher: ActorRef[ComponentMessage], state: SupervisorLifecycleState)
    extends CommandSerializable

object LifecycleStateChanged {
  implicit object NameableLifecycleStateChanged extends Nameable[LifecycleStateChanged] {
    override def name(state: LifecycleStateChanged): StateName = StateName(state.state.toString)
  }
}
