package csw.services.location.internal

import scala.collection.JavaConverters._

object ScalaCompat {
  def toMap[K, V](javaMap: java.util.Map[K, V]): Map[K, V] = javaMap.asScala.toMap
}
