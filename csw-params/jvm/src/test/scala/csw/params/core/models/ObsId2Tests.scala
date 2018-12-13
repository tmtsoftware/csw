package csw.params.core.models

import org.scalatest.{FunSpec, Matchers}

class ObsId2Tests extends FunSpec with Matchers {

  val obsID1 = "2022A-Q-P012-O123"     // without file
  val obsID2 = "2022A-Q-P012-O123-X0234" // with file

  describe("RegEx") {
    //YYYY(A|B|E)-(Q|C)-PXXX-OXXX-XXXX
//    val reg = """(\d{4})(A|B)-(C|Q|P|E)-P(\d{1,3})-O(\d{1,3})(?:-)?(\d+)?""".r
    val reg = raw"(\d{4})(A|B|E)".r

    "2022A" match {
      case reg(year, sem) =>
        println(s"Year: $year $sem")
      case a =>
        println("Its: " + a)
    }

    "2022A-Q-P012-O123" match {
      case reg(year, sem, kind, prog, obs, null) =>
        println(s"Its: $year $sem with $kind and prog $prog, obs $obs")
        println("Kind: " + kind.getClass)

      case reg(year, sem, kind, prog, obs, file) => println(s"Its: $year $sem with $kind and prog $prog, obs $obs, file $file")
      case _                                     => println("Fail")
    }
    "2022A-Q-P012-O123-234" match {
      case reg(year, sem, kind, prog, obs, null) => println(s"Its: $year $sem with $kind and prog $prog, obs $obs")
      case reg(year, sem, kind, prog, obs, file) => println(s"Its: $year $sem with $kind and prog $prog, obs $obs, file $file")
      case _                                     => println("Fail")
    }
  }

  describe("Basdic ObsID Test") {

    val ob1: ObsID2 = obsID1

    println("OB1: " + ob1)
  }
}
