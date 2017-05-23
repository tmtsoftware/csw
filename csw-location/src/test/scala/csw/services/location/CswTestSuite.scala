package csw.services.location

import csw.services.location.commons.TestFutureExtension.RichFuture
import csw.services.logging.{GenericLogger, LoggingSystem, StdOutAppender}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

abstract class CswTestSuite
    extends FunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GenericLogger.Simple {

  private val loggingSystem = LoggingSystem(log, appenderBuilders = Seq(StdOutAppender))

  protected def afterAllTests(): Unit

  override protected def afterAll(): Unit = {
    afterAllTests()
    loggingSystem.stop.await
  }
}
