package csw.database.client.exceptions

case class DatabaseException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)
