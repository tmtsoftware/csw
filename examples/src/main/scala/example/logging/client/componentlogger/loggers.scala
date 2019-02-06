package example.logging.client.componentlogger

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.ComponentMessage
import csw.logging.client.javadsl.JLoggerFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

//#component-logger-class
class SampleClass(loggerFactory: LoggerFactory) {

  val log: Logger = loggerFactory.getLogger
}
//#component-logger-class

//#component-logger-actor
class SampleActor(loggerFactory: LoggerFactory) extends akka.actor.Actor {

  //context is available from akka.actor.Actor
  val log: Logger = loggerFactory.getLogger(context)

  override def receive = ???
}
//#component-logger-actor

//#component-logger-typed-actor
class SampleTypedActor(loggerFactory: LoggerFactory, ctx: ActorContext[ComponentMessage]) {

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
