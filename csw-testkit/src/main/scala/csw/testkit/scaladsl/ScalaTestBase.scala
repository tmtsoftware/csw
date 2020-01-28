package csw.testkit.scaladsl
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, OptionValues, TestSuite}
import org.scalatest.matchers.should.Matchers

trait ScalaTestBase extends TestSuite with Matchers with BeforeAndAfterAll with ScalaFutures with Eventually with OptionValues
