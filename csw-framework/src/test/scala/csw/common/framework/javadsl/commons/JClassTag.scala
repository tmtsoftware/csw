package csw.common.framework.javadsl.commons

import scala.reflect.ClassTag

object JClassTag {
  def make[T](klass: Class[T]): ClassTag[T] = ClassTag(klass)
}
