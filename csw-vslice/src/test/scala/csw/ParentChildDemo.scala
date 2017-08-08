package csw

import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.{EffectfulActorContext, StubbedActorContext}
import akka.typed.{ActorRef, ActorSystem, Behavior}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

sealed trait WorkerInfo {
  val name: String
  val someState: Int
}

sealed trait SupervisorMsg
case class Initialized(ref: ActorRef[InitializedMsgs]) extends SupervisorMsg

sealed trait WorkerMsg
case object InitializeWorker extends WorkerMsg
sealed trait InitializedMsgs extends WorkerMsg
case object DoTask           extends InitializedMsgs

object Supervisor {
  def behavior(workerInfo: WorkerInfo, workerFactory: WorkerFactory): Behavior[SupervisorMsg] =
    Actor.mutable(ctx ⇒ new Supervisor(ctx, workerInfo, workerFactory))
}
class Supervisor(ctx: ActorContext[SupervisorMsg], workerInfo: WorkerInfo, workerFactory: WorkerFactory)
    extends MutableBehavior[SupervisorMsg] {
  val worker: ActorRef[Nothing] = ctx.spawnAnonymous[Nothing](workerFactory.behavior(workerInfo, ctx.self))
  ctx.watch(worker)

  override def onMessage(msg: SupervisorMsg): Behavior[SupervisorMsg] = {
    msg match {
      case Initialized(ref) => ref ! DoTask
    }
    this
  }
}

class SampleWorker(ctx: ActorContext[WorkerMsg], workerInfo: WorkerInfo, supervisor: ActorRef[SupervisorMsg])
    extends MutableBehavior[WorkerMsg] {
  //some state

  ctx.self ! InitializeWorker

  override def onMessage(msg: WorkerMsg): Behavior[WorkerMsg] = {
    msg match {
      case InitializeWorker => //initialize
        supervisor ! Initialized(ctx.self)
      case DoTask ⇒
        println("Message received from supervisor to Do Task")
    }
    this
  }
}

class SampleWorkerFactory extends WorkerFactory {
  def behavior(workerInfo: WorkerInfo, supervisor: ActorRef[SupervisorMsg]): Behavior[Nothing] =
    Actor
      .mutable[WorkerMsg](ctx ⇒ new SampleWorker(ctx, workerInfo, supervisor))
      .narrow
}

abstract class WorkerFactory {
  def behavior(workerInfo: WorkerInfo, supervisor: ActorRef[SupervisorMsg]): Behavior[Nothing]
}

class TestSupervisor extends FunSuite with Matchers with MockitoSugar {
  ignore("test supervisor using EffectfulActorContext") {
    val workerInfo = new WorkerInfo {
      override val someState: Int = 0
      override val name: String   = "test"
    }
    val mockFactory = mock[WorkerFactory]

    val system = ActorSystem("test-system", Supervisor.behavior(workerInfo, mockFactory))
    val ctx = new EffectfulActorContext[SupervisorMsg]("test-supervisor",
                                                       Supervisor.behavior(workerInfo, mockFactory),
                                                       100,
                                                       system)

    ctx.getAllEffects()
  }

  ignore("test supervisor using StubbedActorContext") {
    val workerInfo = new WorkerInfo {
      override val someState: Int = 0
      override val name: String   = "test"
    }
    val system = ActorSystem("test-system", Actor.empty)
    val ctx    = new StubbedActorContext[SupervisorMsg]("test-supervisor", 100, system)

    new Supervisor(ctx, workerInfo, new SampleWorkerFactory)
    ctx.children.toList.length shouldBe 1

    ctx.childInbox(ctx.children.head.upcast[WorkerMsg]).receiveMsg() shouldBe InitializeWorker
  }

}
