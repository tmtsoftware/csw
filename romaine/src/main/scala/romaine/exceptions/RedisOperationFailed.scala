package romaine.exceptions

case class RedisOperationFailed(msg: String, ex: Throwable = None.orNull) extends RuntimeException(msg, ex)
