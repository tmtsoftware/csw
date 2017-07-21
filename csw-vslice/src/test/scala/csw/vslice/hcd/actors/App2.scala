package csw.vslice.hcd.actors

import akka.actor
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.adapter._
import akka.typed.{ActorRef, Behavior}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationServiceFactory}
import csw.trombone.hcd.MotionWorkerMsgs

object App2 extends App {
  private val actorSystem: actor.ActorSystem = ActorSystemFactory.remote()
  private val locationService                = LocationServiceFactory.make()
  private val akkaConnection                 = AkkaConnection(ComponentId("DummyHcd", ComponentType.HCD))

  private val beh                                  = Actor.mutable[MotionWorkerMsgs](_ ⇒ new DummyHcd())
  private val actorRef: ActorRef[MotionWorkerMsgs] = actorSystem.spawn(beh, name = "dummy-hcd")

  private val akkaRegistration = AkkaRegistration(akkaConnection, actorRef.toUntyped)

  locationService.register(akkaRegistration)
}

class DummyHcd() extends Actor.MutableBehavior[MotionWorkerMsgs] {
  override def onMessage(msg: MotionWorkerMsgs): Behavior[MotionWorkerMsgs] = {
    msg match {
      case x @ _ ⇒
        println(s"Received command--$x")
        this
    }
  }
}
