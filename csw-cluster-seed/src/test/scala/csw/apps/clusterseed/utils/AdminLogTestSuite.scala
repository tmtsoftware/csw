package csw.apps.clusterseed.utils

import java.net.InetAddress

import com.persist.JsonOps.{Json, JsonObject}
import csw.apps.clusterseed.admin.internal.AdminWiring
import csw.messages.models.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.internal.LoggingSystem
import csw.services.logging.scaladsl.LoggingSystemFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class AdminLogTestSuite extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  protected val logBuffer: mutable.Buffer[JsonObject] = mutable.Buffer.empty[JsonObject]
  protected val testAppender                          = new TestAppender(x ⇒ logBuffer += Json(x.toString).asInstanceOf[JsonObject])

  protected val hostName: String = InetAddress.getLocalHost.getHostName

  protected val adminWiring: AdminWiring = AdminWiring.make(ClusterAwareSettings.onPort(3552), None)
  protected val loggingSystem: LoggingSystem =
    LoggingSystemFactory.start("logging", "version", hostName, adminWiring.actorSystem)

  loggingSystem.setAppenders(List(testAppender))

  override protected def beforeAll(): Unit = {
    logBuffer.clear()
    Await.result(adminWiring.adminHttpService.registeredLazyBinding, 5.seconds)
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  override protected def afterAll(): Unit = {
    Await.result(loggingSystem.stop, 10.seconds)
    Await.result(adminWiring.actorRuntime.shutdown(TestFinishedReason), 10.seconds)
  }

}
