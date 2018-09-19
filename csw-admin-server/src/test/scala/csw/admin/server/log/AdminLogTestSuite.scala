package csw.admin.server.log

import java.net.InetAddress

import csw.location.server.http.HTTPLocationService
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable

trait AdminLogTestSuite extends FunSuite with Matchers with HTTPLocationService {

  protected val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  protected val testAppender                        = new TestAppender(x ⇒ logBuffer += Json.parse(x.toString).as[JsObject])

  protected val hostName: String = InetAddress.getLocalHost.getHostName
}
