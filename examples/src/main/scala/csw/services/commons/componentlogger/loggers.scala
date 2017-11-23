package csw.services.commons.componentlogger

import akka.typed.scaladsl.ActorContext
import csw.services.commons.ComponentDomainMessage
import csw.services.logging.javadsl.JLoggerFactory
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

//#component-logger-class
class SampleClass(loggerFactory: LoggerFactory) {

  val log: Logger = loggerFactory.getLogger
}
//#component-logger-class

//#component-logger-actor
class SampleActor(loggerFactory: LoggerFactory) extends akka.actor.Actor {

  val log: Logger = loggerFactory.getLogger(context)

  override def receive = ???
}
//#component-logger-actor

//#component-logger-typed-actor
class SampleTypedActor(loggerFactory: LoggerFactory, ctx: ActorContext[ComponentDomainMessage]) {

  val log: Logger = loggerFactory.getLogger(ctx)
}
//#component-logger-typed-actor

object Sample {
  //#logger-factory-creation
  val loggerFactory: LoggerFactory = new LoggerFactory("my-component-name")

  // convert a scala LoggerFactory to java JLoggerFactory
  val jLoggerFactory: JLoggerFactory = loggerFactory.asJava
  //#logger-factory-creation
}
