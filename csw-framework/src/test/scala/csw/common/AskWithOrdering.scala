package csw.common

import akka.Done
import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.util.Timeout
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationLong

class AskWithOrdering extends FunSuite with Matchers {

  private implicit val test: ActorSystem = ActorSystem("test")

  private val actorRef: ActorRef[Accumulator.Msg] = test.spawnAnonymous(Accumulator.beh)
  private val service                             = new Accumulator.Service(actorRef)

  private implicit val timeout: Timeout = Timeout(5.seconds)
  import test.dispatcher

  test("ddd ask should maintain ordering") {
    (1 to 10).foreach(service.put)
    val results = Await.result(service.get, 5.seconds)
    println(results)
    results.reverse shouldBe (1 to 100)
  }

  test("eee ask from different threads should not maintain ordering") {
    Future((1 to 100).foreach(service.put))
    val results = Await.result(service.get, 5.seconds)
    println(results)
    results.reverse shouldBe (1 to 1000)
  }

  object Accumulator {
    trait Msg
    case class Put(x: Int)(val replyTo: ActorRef[Done]) extends Msg
    case class Get(replyTo: ActorRef[List[Int]])        extends Msg

    val beh: Behavior[Msg] = Behaviors.setup { _ =>
      var xs: List[Int] = Nil
      Behaviors.receiveMessage[Msg] {
        case p @ Put(x) =>
          xs = x :: xs
          p.replyTo ! Done
          Behaviors.same
        case Get(replyTo) =>
          replyTo ! xs
          Behaviors.same
      }
    }

    class Service(actorRef: ActorRef[Msg])(implicit val actorSystem: ActorSystem) {
      implicit val scheduler: Scheduler = actorSystem.scheduler
      import akka.actor.typed.scaladsl.AskPattern._
      def put(x: Int)(implicit timeout: Timeout): Future[Done] = actorRef ? Put(x)
      def get(implicit timeout: Timeout): Future[List[Int]]    = actorRef ? Get
    }
  }
}
