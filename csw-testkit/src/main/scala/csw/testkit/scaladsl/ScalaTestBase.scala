package csw.testkit.scaladsl
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, Matchers, TestSuite}

trait ScalaTestBase extends TestSuite with Matchers with BeforeAndAfterAll with ScalaFutures with Eventually
