package csw.vslice.hcd.actors

import akka.actor
import akka.typed.scaladsl.adapter._
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.services.location.scaladsl.{ActorSystemFactory, LocationServiceFactory}
import csw.trombone.hcd.MotionWorkerMsgs.Start
import csw.trombone.hcd.actors.MotionWorker

import scala.concurrent.ExecutionContext.Implicits.global

object App1 extends App {

  private val actorSystem: actor.ActorSystem = ActorSystemFactory.remote()
  private val locationService                = LocationServiceFactory.make()
  private val akkaConnection                 = AkkaConnection(ComponentId("DummyHcd", ComponentType.HCD))

  Thread.sleep(3000)

  locationService
    .find(akkaConnection)
    .foreach(mayBeLocation ⇒ {
      val loc: AkkaLocation = mayBeLocation.get.asInstanceOf[AkkaLocation]
      val beh               = MotionWorker.behaviour(0, 10, 1000, loc.actorRef, true)

      val actorRef = actorSystem.spawn(beh, "test1")
      actorRef ! Start(loc.actorRef)
    })

}
