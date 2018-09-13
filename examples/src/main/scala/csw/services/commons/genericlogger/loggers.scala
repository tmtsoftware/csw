package csw.services.commons.genericlogger

import akka.actor.typed.scaladsl.ActorContext
import csw.command.messages.ComponentMessage
import csw.services.logging.scaladsl.{GenericLoggerFactory, Logger}

//#generic-logger-class
class GenericClass {

  val log: Logger = GenericLoggerFactory.getLogger
}
//#generic-logger-class

//#generic-logger-actor
class GenericActor extends akka.actor.AbstractActor {

  //context is available from akka.actor.Actor
  val log: Logger = GenericLoggerFactory.getLogger(context)

  override def createReceive() = ???
}
//#generic-logger-actor

//#generic-logger-typed-actor
class GenericTypedActor(ctx: ActorContext[ComponentMessage]) {

  val log: Logger = GenericLoggerFactory.getLogger(ctx)
}
//#generic-logger-typed-actor
