package csw.messages.models

import csw.messages.models.params.ObsId2
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2
import org.scalatest.prop.Tables.Table
import org.scalatest.{FunSuite, Matchers}

import scala.util.matching.Regex

class ObsIdTest extends FunSuite with Matchers {

  val obsID1 = "2022A-Q-P012-O123"     // without file
  val obsID2 = "2022A-Q-P012-O123-234" // with file

  val testData: TableFor2[String, ObsId2] = Table(
    ("obsId_String", "obsId_model"),
    ("2022A-Q-P012-O123", ObsId2("2022", "A", "Q", "012", "123", None)),
    ("2022A-Q-P012-O123-234", ObsId2("2022", "A", "Q", "012", "123", Some("234")))
  )

  def parseToObsId(str: String, reg: Regex): Option[ObsId2] = str match {
    case reg(year, sem, kind, prog, obs, null) => Some(ObsId2(year, sem, kind, prog, obs, None))
    case reg(year, sem, kind, prog, obs, file) => Some(ObsId2(year, sem, kind, prog, obs, Some(file)))
    case _                                     => None
  }

  test("RegEx") {
    //YYYY(A|B|E)-(Q|C)-PXXX-OXXX-XXXX
    val reg = """(\d{4})(A|B|E)-(C|Q)-P(\d{1,3})-O(\d{1,3})(?:-)?(\d+)?""".r

    forAll(testData) { (obsId: String, obsId2: ObsId2) ⇒
      parseToObsId(obsId, reg).get shouldBe obsId2
    }
  }

  test("Basdic ObsID Test") {
    val ob1: ObsId2 = obsID1
    assert(ob1 == ObsId2("2022", "A", "Q", "012", "123", None))
  }

}
