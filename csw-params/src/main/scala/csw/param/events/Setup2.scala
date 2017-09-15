package csw.param.events

import csw.param.generics.Parameter
import spray.json.JsonFormat

import scala.reflect.ClassTag

case class Setup2[S: JsonFormat: ClassTag](params: Array[Parameter[S]])

object Setup2 {}
