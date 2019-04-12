package csw.logging.client.utils

import akka.actor.typed.ActorSystem
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import play.api.libs.json.JsObject

class TestAppender(callback: Any ⇒ Unit) extends LogAppenderBuilder {

  /**
   * A constructor for the TestAppender class.
   *
   * @param system    an Akka factory.
   * @param stdHeaders the headers that are fixes for this service.
   * @return the stdout appender.
   */
  def apply(system: ActorSystem[_], stdHeaders: JsObject): StdOutAppender =
    new StdOutAppender(system, stdHeaders, callback)
}
