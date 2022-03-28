/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.commons

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}

import scala.language.reflectiveCalls

abstract class AskProxyTestKit[Msg, Impl](implicit actorSystem: ActorSystem[_]) {
  protected def make(actorRef: ActorRef[Msg]): Impl

  def withBehavior(pf: PartialFunction[Msg, Unit]): Assertable = {
    var requestReceived = false
    val behavior = Behaviors.receiveMessagePartial[Msg] { req =>
      requestReceived = true
      if (pf.isDefinedAt(req)) pf(req) else senderOf(req) ! s"Unhandled message: $req"
      Behaviors.same
    }
    val stubActorRef = actorSystem.systemActorOf(behavior, s"ask-test-kit-stub-$uuid")
    val proxy        = make(stubActorRef)
    assertion => {
      assertion(proxy)
      assert(requestReceived, s"mocked request was not received")
    }
  }

  private def senderOf(req: Msg) = req.asInstanceOf[{ def replyTo: ActorRef[Any] }].replyTo
  private def uuid               = UUID.randomUUID.toString

  trait Assertable {
    def check(assertion: Impl => Unit): Unit
  }
}
