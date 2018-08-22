package romaine.exceptions

case class RedisServerNotAvailable(cause: Throwable) extends RuntimeException("Redis Server not available", cause)
