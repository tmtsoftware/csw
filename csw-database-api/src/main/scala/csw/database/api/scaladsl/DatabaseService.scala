package csw.database.api.scaladsl
import java.sql.{DatabaseMetaData, ResultSet}

import scala.concurrent.Future

trait DatabaseService {

  // Used to execute queries like create, update, delete that does not return any ResultSet
  def execute(sql: String): Future[Unit]

  // Used to execute select type of queries that return ResultSet
  def executeQuery(sql: String): Future[ResultSet]

  // Used to get meta data for the connection being used
  def getConnectionMetaData: Future[DatabaseMetaData]
}
