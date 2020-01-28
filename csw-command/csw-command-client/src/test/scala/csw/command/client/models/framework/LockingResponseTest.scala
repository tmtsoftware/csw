package csw.command.client.models.framework

import csw.command.client.models.framework.LockingResponse.{
  LockAcquired,
  LockAlreadyReleased,
  LockExpired,
  LockExpiringShortly,
  LockReleased
}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LockingResponseTest extends AnyFunSuite with Matchers {

  test("verify java API's mapping to scala API's") {
    LockingResponse.lockAcquired should ===(LockAcquired)
    LockingResponse.lockAlreadyReleased should ===(LockAlreadyReleased)
    LockingResponse.lockExpiringShortly should ===(LockExpiringShortly)
    LockingResponse.lockExpired should ===(LockExpired)
    LockingResponse.lockReleased should ===(LockReleased)
  }

}
