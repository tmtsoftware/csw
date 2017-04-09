package csw.services.config.server.http

import java.io.{FileNotFoundException, IOException}

object ClientException {

  def apply(t: Throwable): Boolean = t match {
    case _: IOException | _: FileNotFoundException ⇒ true
    case _ ⇒ false
  }

  def unapply(t: Throwable): Option[Throwable] = if(apply(t)) Some(t) else None
}
