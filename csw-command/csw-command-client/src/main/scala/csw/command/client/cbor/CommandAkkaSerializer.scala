/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.command.client.cbor

import csw.command.client.messages._
import csw.command.client.messages.sequencer.CswSequencerMessage
import csw.command.client.models.framework._
import csw.commons.CborAkkaSerializer
import csw.params.commands.CommandResponse
import csw.params.core.states.StateVariable
import csw.serializable.CommandSerializable

class CommandAkkaSerializer extends CborAkkaSerializer[CommandSerializable] with MessageCodecs {

  override def identifier: Int = 19923

  register[CommandSerializationMarker.RemoteMsg]
  register[CommandResponse]
  register[StateVariable]
  register[SupervisorLifecycleState]
  register[ContainerLifecycleState]
  register[LifecycleStateChanged]
  register[Components]
  register[LockingResponse]
  register[CswSequencerMessage]
}
