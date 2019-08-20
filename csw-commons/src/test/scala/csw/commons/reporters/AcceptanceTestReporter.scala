package csw.commons.reporters

import java.text.SimpleDateFormat
import java.util.Date

import org.scalatest.Reporter
import org.scalatest.events._

import scala.collection.mutable

class AcceptanceTestReporter extends Reporter {
  private val data      = mutable.ListBuffer[List[Any]]()
  private val dateTime  = new Date
  private val timestamp = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss").format(dateTime)

  override def apply(event: Event): Unit = event match {
    case _: RunStarting =>
      data.clear()
      data.append(List("CSW Acceptance Test Results", timestamp))
      data.append(List("Class", "Test Name", "Result"))
    case _: RunCompleted => printData()
    case _: RunAborted   => printData()
    case e: SuiteStarting =>
      println(s"${e.suiteClassName.getOrElse("NoClass")} starting")
    case e: SuiteAborted =>
      println(s"${e.suiteClassName} aborted")
      data.append(List(e.suiteClassName.getOrElse("NoClass"), "NONE", "ABORTED"))
    case e: SuiteCompleted =>
      println(s"${e.suiteClassName.getOrElse("NoClass")} completed")
    case e: TestSucceeded =>
      println(s"${e.suiteClassName.getOrElse("NoClass")}, ${e.testName}, PASSED")
      data.append(List(e.suiteClassName.getOrElse("NoClass"), e.testName, "PASSED"))
    case e: TestFailed =>
      println(s"${e.suiteClassName.getOrElse("NoClass")}, ${e.testName}, FAILED, <<<${e.message}>>>")
      data.append(List(e.suiteClassName.getOrElse("NoClass"), e.testName, "PASSED"))

    case e => println(e.toString)
  }

  private def printData(): Unit = {
    println("=" * 80)
    data.foreach(println)
  }
}
