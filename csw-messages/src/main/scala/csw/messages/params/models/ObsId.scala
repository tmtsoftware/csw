package csw.messages.params.models

import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.matching.Regex

/**
 * Supports ObsID of the form 2022A|B-Q|C-ProgNum-ObsNum-FileNum
 *                            YYYY(A|B)-(Q|C)-PXXX-OXXX-FXXXX
 *                            0123 4   5 6   7890123456789
 *                                              1111111111
 */
sealed trait ProgramKind
case object Classical          extends ProgramKind
case object PreProgrammedQueue extends ProgramKind

object ObsId {

  implicit val format: Format[ObsId] = new Format[ObsId] {
    override def writes(obj: ObsId): JsValue           = JsString(obj.obsId)
    override def reads(json: JsValue): JsResult[ObsId] = JsSuccess(ObsId(json.as[String]))
  }

  def empty: ObsId = ObsId("")
}

case class ObsId(obsId: String)

case class ObsId2(year: String, sem: String, kind: String, prog: String, obs: String, file: Option[String]) {
  // private final val PROG_KIND_INDEX = 6

  //def isQueue = obsId.charAt(PROG_KIND_INDEX) == PreProgrammedQueueKindLetter

  //def isClassical = obsId.charAt(PROG_KIND_INDEX) == ClassicalKindLetter
  /*
  def progType: ProgramKind = obsId.charAt(PROG_KIND_INDEX) match {
    case ClassicalKindLetter => Classical
    case PreProgrammedQueueKindLetter => PreProgrammedQueue
  }
 */
}

object ObsId2 {
  val BAD_OBSID: ObsId2 = ObsId2("bad", "obs", "id", "bad", "obs", Some("id"))

  //YYYY(A|B|E)-(Q|C)-PXXX-OXXX-XXXX
  val reg: Regex = """(\d{4})(A|B|E)-(C|Q)-P(\d{1,3})-O(\d{1,3})(?:-)?(\d+)?""".r

  implicit def create(obsId: String): ObsId2 = obsId match {
    case reg(year, sem, kind, prog, obs, null) =>
      //      println(s"Its: $year $sem with $kind and prog $prog, obs $obs")
      ObsId2(year, sem, kind, prog, obs, None)
    case reg(year, sem, kind, prog, obs, file) =>
      //      println(s"Its: $year $sem with $kind and prog $prog, obs $obs, file $file")
      ObsId2(year, sem, kind, prog, obs, Some(file))
    case _ =>
      println("Fail")
      BAD_OBSID
  }
}
