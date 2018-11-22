package csw.database.api.javadasl

import java.sql.{DatabaseMetaData, ResultSet}
import java.util.concurrent.CompletableFuture

trait IDatabaseService {

  // Used to execute queries like create, update, delete that does not return any ResultSet
  def execute(sql: String): CompletableFuture[Unit]

  // Used to execute select type of queries that return ResultSet
  def executeQuery(sql: String): CompletableFuture[ResultSet]

  // Used to get meta data for the connection being used
  def getConnectionMetaData: CompletableFuture[DatabaseMetaData]

  def closeConnection(): CompletableFuture[Unit]
}
