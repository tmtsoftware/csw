package tmt.approach2

import java.io.File

import scala.io.Source
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object ScriptLoader {
  def fromFile(file: File): Runnable = fromString {
    val template = Source.fromResource("approach2/main.sc").mkString
    val script   = Source.fromFile(file).mkString
    template.replace("<script/>", script)
  }

  def fromString(code: String): Runnable = {
    val tb        = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()
    val testClass = tb.compile(tb.parse(code))().asInstanceOf[Class[_]]
    testClass.getDeclaredConstructor().newInstance().asInstanceOf[Runnable]
  }
}
