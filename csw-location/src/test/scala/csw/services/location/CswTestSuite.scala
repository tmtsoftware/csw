package csw.services.location

import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.logging.LoggingSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

abstract class CswTestSuite extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  private val loggingSystem = LoggingSystem()

  protected def afterAllTests(): Unit

  override protected def afterAll(): Unit = {
    afterAllTests()
    loggingSystem.stop.await
  }
}
