package csw.services.commons.immutablelogger

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.services.commons.commonlogger.ExampleLogger
import csw.services.logging.scaladsl.{ComponentLogger, GenericLogger}

object ComponentImmutableLogger {
  def behavior(_componentName: String): Behavior[_] = Actor.immutable[_] { (ctx, msg) ⇒
    val log = ComponentLogger.immutable(ctx, _componentName)
    Actor.same
  }
}

object CommonImmutableLogger {
  def behavior(): Behavior[_] = Actor.immutable[_] { (ctx, msg) ⇒
    val log = ExampleLogger.immutable(ctx)
    Actor.same
  }
}

object GenericImmutableLogger {
  def behavior(): Behavior[_] = Actor.immutable[_] { (ctx, msg) ⇒
    val log = GenericLogger.immutable(ctx)
    Actor.same
  }
}
