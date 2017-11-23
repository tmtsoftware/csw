package csw.services.commons.immutablelogger

import akka.typed.Behavior
import akka.typed.scaladsl.Actor
import csw.services.commons.ComponentDomainMessage
import csw.services.commons.commonlogger.SampleLogger
import csw.services.logging.scaladsl.{GenericLoggerFactory, LoggerFactory}

//#common-component-logger
object CommonImmutableSample {
  def behavior(): Behavior[ComponentDomainMessage] = Actor.immutable[ComponentDomainMessage] { (ctx, msg) ⇒
    val log = SampleLogger.getLogger(ctx)

    log.info(s"Received msg: [$msg]")
    Actor.same
  }
}
//#common-component-logger

//#component-logger
object ImmutableSample {
  def behavior(_componentName: String): Behavior[ComponentDomainMessage] = Actor.immutable[ComponentDomainMessage] { (ctx, msg) ⇒
    val log = new LoggerFactory(_componentName).getLogger(ctx)

    log.info(s"Received msg: [$msg]")
    Actor.same
  }
}
//#component-logger

//#generic-logger
object GenericImmutableSample {
  def behavior(): Behavior[ComponentDomainMessage] = Actor.immutable[ComponentDomainMessage] { (ctx, msg) ⇒
    val log = GenericLoggerFactory.getLogger(ctx)

    log.info(s"Received msg: [$msg]")
    Actor.same
  }
}
//#generic-logger
