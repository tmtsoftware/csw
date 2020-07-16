package csw.framework.models

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}

import scala.concurrent.ExecutionContextExecutor

abstract class ComponentContext[T] {
  implicit def executionContext: ExecutionContextExecutor

  def self: ActorRef[T]
  def system: ActorSystem[_]

  def spawnAnonymous[U](behavior: Behavior[U]): ActorRef[U] = spawnAnonymous(behavior, Props.empty)
  def spawnAnonymous[U](behavior: Behavior[U], props: Props): ActorRef[U]

  def spawn[U](behavior: Behavior[U], name: String): ActorRef[U] = spawn(behavior, name, Props.empty)
  def spawn[U](behavior: Behavior[U], name: String, props: Props): ActorRef[U]
}

object ComponentContext {
  def from[T](ctx: ActorContext[T]): ComponentContext[T] = {
    new ComponentContext[T] {
      override implicit def executionContext: ExecutionContextExecutor = ctx.executionContext

      override def self: ActorRef[T]      = ctx.self
      override def system: ActorSystem[_] = ctx.system

      override def spawnAnonymous[U](behavior: Behavior[U], props: Props): ActorRef[U]      = ctx.spawnAnonymous(behavior, props)
      override def spawn[U](behavior: Behavior[U], name: String, props: Props): ActorRef[U] = ctx.spawn(behavior, name, props)
    }
  }
}
