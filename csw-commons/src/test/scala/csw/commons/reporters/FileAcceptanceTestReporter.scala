package csw.commons.reporters

import java.io.{File, FileWriter}

import org.scalatest.Reporter
import org.scalatest.events._

import scala.collection.mutable

class FileAcceptanceTestReporter extends Reporter {
  private val data = mutable.ListBuffer[String]()
  private val delimiter = "\t"

  private val filePath = s"target/acceptance/acceptance-test-report.txt"
  private val file = new File(filePath)
  if (file.exists()) file.delete()
  file.getParentFile.mkdirs
  private val writer = new FileWriter(file, true)

  override def apply(event: Event): Unit = event match {
    case _: RunStarting  => data.clear()
    case _: RunCompleted => writeData()
    case _: RunAborted   => writeData()
    case e: SuiteAborted =>
      data.append(List(e.suiteClassName.getOrElse("NoClass"), "NONE", "ABORTED").mkString(delimiter))
    case e: TestSucceeded =>
      data.append(List(e.suiteClassName.getOrElse("NoClass"), e.testName, "PASSED").mkString(delimiter))
    case e: TestFailed =>
      data.append(List(e.suiteClassName.getOrElse("NoClass"), e.testName, "FAILED").mkString(delimiter))
    case e: TestIgnored =>
      data.append(List(e.suiteClassName.getOrElse("NoClass"), e.testName, "IGNORED").mkString(delimiter))
    case e: TestCanceled =>
      data.append(List(e.suiteClassName.getOrElse("NoClass"), e.testName, "CANCELED").mkString(delimiter))
    case _ =>
  }

  private def writeData(): Unit = {
    data.foreach(l => writer.write(s"$l\n"))
    writer.close()
  }
}
