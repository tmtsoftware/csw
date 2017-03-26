package csw.services.location.helpers

import akka.remote.testkit.{MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit.ImplicitSender
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

abstract class LSMultiNodeSpec extends MultiNodeSpec(LSMultiNodeConfig, LSMultiNodeConfig.makeSystem)
  with ImplicitSender
  with MultiNodeSpecCallbacks
  with FunSuiteLike
  with Matchers
  with BeforeAndAfterAll {

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()
}
