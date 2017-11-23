package csw.services.commons.genericlogger

import akka.typed.scaladsl.ActorContext
import csw.services.commons.ComponentDomainMessage
import csw.services.logging.scaladsl.{GenericLoggerFactory, Logger}

//#generic-logger-class
class GenericClass {

  val log: Logger = GenericLoggerFactory.getLogger
}
//#generic-logger-class

//#generic-logger-actor
class GenericActor extends akka.actor.AbstractActor {

  val log: Logger = GenericLoggerFactory.getLogger(context)

  override def createReceive() = ???
}
//#generic-logger-actor

//#generic-logger-typed-actor
class GenericTypedActor(ctx: ActorContext[ComponentDomainMessage]) {

  val log: Logger = GenericLoggerFactory.getLogger(ctx)
}
//#generic-logger-typed-actor
