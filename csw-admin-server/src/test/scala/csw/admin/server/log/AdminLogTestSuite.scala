package csw.admin.server.log

import java.net.InetAddress

import com.persist.JsonOps.{Json, JsonObject}
import csw.location.server.http.HTTPLocationService
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable

trait AdminLogTestSuite extends FunSuite with Matchers with HTTPLocationService {

  protected val logBuffer: mutable.Buffer[JsonObject] = mutable.Buffer.empty[JsonObject]
  protected val testAppender                          = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])

  protected val hostName: String = InetAddress.getLocalHost.getHostName
}
