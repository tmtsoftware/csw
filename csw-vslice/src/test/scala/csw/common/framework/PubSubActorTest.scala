package csw.common.framework

import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem, Behavior}
import csw.common.framework.A.Submit
import org.scalatest.{FunSuite, Matchers}

object A {
  case class Submit(replyTo: ActorRef[Int], n: Int)

  def aa(sum: Int): Actor.Immutable[Submit] = Actor.immutable[Submit] {
    case (s, Submit(replyTo, n)) ⇒
      println(n)
      replyTo ! sum + n
      aa(sum + n)
  }

  def beh: Behavior[Submit] = Actor.mutable(ctx ⇒ new B)

}

class B extends MutableBehavior[A.Submit] {
  override def onMessage(msg: A.Submit): Behavior[A.Submit] = loop(msg, 33)

  def loop(msg: A.Submit, sum: Int): Behavior[A.Submit] = msg match {
    case Submit(replyTo, n) ⇒
      println(n)
      replyTo ! sum + n
      loop(msg, sum + n)
  }
}

class PubSubActorTest extends FunSuite with Matchers {

  test("demo") {
    val system   = ActorSystem("test", A.aa(33))
    val settings = TestKitSettings(system)
    val probe    = TestProbe[Int]()(system, settings)

    system ! A.Submit(probe.ref, 100)
    probe.expectMsg(133)

    system ! A.Submit(probe.ref, 100)
    probe.expectMsg(233)
  }

  test("demo2") {
    val system   = ActorSystem("test", A.beh)
    val settings = TestKitSettings(system)
    val probe    = TestProbe[Int]()(system, settings)

    system ! A.Submit(probe.ref, 100)
    probe.expectMsg(133)

    system ! A.Submit(probe.ref, 100)
    probe.expectMsg(233)
  }

}
