package csw.acceptance.runner

import java.net.URLClassLoader

import csw.commons.tagobjects.ClasspathSensitive
import org.scalatest.tools.Runner

object RunAll {
  def main(args: Array[String]): Unit = {

    val testJarsRunpath = getClass.getClassLoader
      .asInstanceOf[URLClassLoader]
      .getURLs
      .map(_.getPath)
      .filter(_.contains("tests.jar"))
      .mkString("\"", " ", "\"")

    val params = Array("-o", "-l", ClasspathSensitive.name, "-R", testJarsRunpath)

    Runner.main(params)
  }
}
