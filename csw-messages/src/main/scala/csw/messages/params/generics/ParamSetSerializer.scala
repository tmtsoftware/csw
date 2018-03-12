package csw.messages.params.generics

import com.twitter.chill.KryoInjection

/**
 * Defines methods for serializing parameter sets
 */
private[messages] object ParamSetSerializer {
  def read[A](bytes: Array[Byte]): A = KryoInjection.invert(bytes).get.asInstanceOf[A]
  def write[A](in: A): Array[Byte]   = KryoInjection(in)
}
