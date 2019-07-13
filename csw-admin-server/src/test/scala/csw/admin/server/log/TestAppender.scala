package csw.admin.server.log

import akka.actor.typed.ActorSystem
import csw.logging.client.appenders.{LogAppenderBuilder, StdOutAppender}
import play.api.libs.json.JsObject

class TestAppender(callback: Any => Unit) extends LogAppenderBuilder {
  def apply(system: ActorSystem[_], stdHeaders: JsObject): StdOutAppender = new StdOutAppender(system, stdHeaders, callback)
}
