package csw.messages.extensions

import java.util.Optional

object OptionConverters {
  implicit class RichOption[T](val underlying: Option[T]) extends AnyVal {
    def asJava: Optional[T] = underlying match { case Some(a) => Optional.ofNullable(a); case _ => Optional.empty[T] }
  }

  implicit class RichOptional[T](val underlying: Optional[T]) extends AnyVal {
    def asScala: Option[T] = if (underlying.isPresent) Some(underlying.get) else None
  }
}
