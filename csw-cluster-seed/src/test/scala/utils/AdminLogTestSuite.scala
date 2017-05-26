package utils

import java.net.InetAddress

import com.persist.JsonOps.{Json, JsonObject}
import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.LoggingSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class AdminLogTestSuite() extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {
  protected val adminWiring = AdminWiring.make(ClusterAwareSettings.onPort(3552))
  protected val httpService = adminWiring.adminHttpService

  protected val logBuffer    = mutable.Buffer.empty[JsonObject]
  protected val testAppender = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])

  protected val hostName      = InetAddress.getLocalHost.getHostName
  protected val loggingSystem = new LoggingSystem("logging", "SNAPSHOT-1.0", hostName, Seq(testAppender))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    logBuffer.clear()
    Await.result(httpService.registeredLazyBinding, 5.seconds)
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = {
    Await.result(adminWiring.actorRuntime.shutdown(), 10.seconds)
    Await.result(loggingSystem.stop, 10.seconds)
  }

}
