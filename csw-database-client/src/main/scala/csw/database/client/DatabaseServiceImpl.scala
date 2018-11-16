package csw.database.client
import java.sql.{Connection, ResultSet, Statement}

import csw.database.api.scaladsl.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

class DatabaseServiceImpl(connection: Connection)(implicit ec: ExecutionContext) extends DatabaseService {
  private val statement: Statement = connection.createStatement()
  override def execute(sql: String): Future[Boolean] = Future {
    statement.execute(sql)
  }

  override def executeQuery(sql: String): Future[ResultSet] = Future {
    statement.executeQuery(sql)
  }
}
