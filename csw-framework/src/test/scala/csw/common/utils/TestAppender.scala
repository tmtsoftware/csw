package csw.common.utils

import akka.actor.ActorRefFactory
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import play.api.libs.json.JsObject

class TestAppender(callback: Any ⇒ Unit) extends LogAppenderBuilder {

  /**
   * A constructor for the TestAppender class.
   *
   * @param factory    an Akka factory.
   * @param stdHeaders the headers that are fixes for this service.
   * @return the stdout appender.
   */
  def apply(factory: ActorRefFactory, stdHeaders: JsObject): StdOutAppender =
    new StdOutAppender(factory, stdHeaders, callback)
}
