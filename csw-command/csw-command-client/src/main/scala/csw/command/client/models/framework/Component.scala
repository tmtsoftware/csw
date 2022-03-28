/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.models.framework

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage

/**
 * A class that represents a logical component with it's supervisor actor reference and it's meta information
 *
 * @param supervisor an actorRef supervising this component
 * @param info all information regarding this component
 */
case class Component private[csw] (supervisor: ActorRef[ComponentMessage], info: ComponentInfo)
