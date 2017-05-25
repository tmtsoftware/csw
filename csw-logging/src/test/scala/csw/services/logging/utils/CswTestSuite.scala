package csw.services.logging.utils

import csw.services.logging.scaladsl.{GenericLogger, LoggingSystemFactory}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

abstract class CswTestSuite
    extends FunSuite
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with GenericLogger.Simple {

  private val loggingSystem = LoggingSystemFactory.start()

  protected def afterAllTests(): Unit

  override protected def afterAll(): Unit = {
    afterAllTests()
    Await.result(loggingSystem.stop, 5.seconds)
  }
}
