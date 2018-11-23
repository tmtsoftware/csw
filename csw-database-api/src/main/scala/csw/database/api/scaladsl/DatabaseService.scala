package csw.database.api.scaladsl
import java.sql.ResultSet

import scala.concurrent.Future

trait DatabaseService {

  // Used to execute queries like create, update, delete that does not return any ResultSet
  def execute(sql: String): Future[Unit]

  // Used to execute select type of queries that return ResultSet
  def executeQuery(sql: String): Future[ResultSet]

  def closeConnection(): Future[Unit]
}
