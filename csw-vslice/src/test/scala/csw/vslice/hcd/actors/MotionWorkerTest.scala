package csw.vslice.hcd.actors

import akka.typed.scaladsl.Actor
import akka.typed.{ActorSystem, Behavior}
import csw.trombone.hcd.MotionWorkerMsgs
import csw.trombone.hcd.MotionWorkerMsgs.Start
import csw.trombone.hcd.actors.MotionWorker
import org.scalatest.FunSuite

class MotionWorkerTest extends FunSuite {
  test("testOnMessage") {

    val sender = ActorSystem("test1", Actor.mutable[MotionWorkerMsgs](_ ⇒ new DummyHcd))
    val system: ActorSystem[MotionWorkerMsgs] =
      ActorSystem("test", MotionWorker.behaviour(0, 10, 1000, sender, true))

//    val probe                                           = TestProbe[MotionWorkerMsgs]("testprobe")(system, TestKitSettings(system))
//    val motionWorkerTestRef: ActorRef[MotionWorkerMsgs] = probe.testActor
//
//    println(s"$sender -- $motionWorkerTestRef")
//
//    motionWorkerTestRef ! Start(sender)
//
//    probe.expectMsg[MotionWorkerMsgs](Start(sender))

    system ! Start(sender)

  }

}

class Sender() extends Actor.MutableBehavior[MotionWorkerMsgs] {
  override def onMessage(msg: MotionWorkerMsgs): Behavior[MotionWorkerMsgs] = {
    msg match {
      case Start(replyTo) ⇒
        println("Received Start in sender")
        this
      case _ ⇒
        println("")
        this
    }
  }
}
