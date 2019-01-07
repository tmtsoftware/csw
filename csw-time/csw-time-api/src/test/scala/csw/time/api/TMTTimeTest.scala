package csw.time.api

import java.time.{Duration, Instant}

import csw.time.api.models.{TAITime, TMTTime, UTCTime}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.Matchers._

class TMTTimeTest extends FunSuite with BeforeAndAfterAll {
  private val TaiOffset = 37

  override protected def beforeAll(): Unit = TAITime.setOffset(TaiOffset)

  test("should get utc time") {
    val utcTime        = UTCTime.now()
    val fixedInstant   = Instant.now()
    val expectedMillis = fixedInstant.toEpochMilli

    utcTime.value.toEpochMilli.toDouble shouldEqual expectedMillis.toDouble +- 5
  }

  test("should convert utc to tai") {

    val utcTime = UTCTime.now()
    val taiTime = utcTime.toTAI
    Duration.between(utcTime.value, taiTime.value).getSeconds shouldEqual TaiOffset
  }
}
