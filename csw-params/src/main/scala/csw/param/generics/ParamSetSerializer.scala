package csw.param.generics

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

/**
 * Defines methods for serializing parameter sets
 */
object ParamSetSerializer {

  def read[A](bytes: Array[Byte]): A = {
    val in  = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val obj = in.readObject()
    in.close()
    obj.asInstanceOf[A]
  }

  def write[A](in: A): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val out = new ObjectOutputStream(bos)
    out.writeObject(in)
    out.close()
    bos.toByteArray
  }
}
