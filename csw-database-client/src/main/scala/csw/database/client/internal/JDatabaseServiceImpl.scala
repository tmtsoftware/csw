package csw.database.client.internal

import java.sql.{DatabaseMetaData, ResultSet}
import java.util.concurrent.CompletableFuture

import csw.database.api.javadasl.IDatabaseService
import csw.database.api.scaladsl.DatabaseService

import scala.compat.java8.FutureConverters.FutureOps

class JDatabaseServiceImpl(databaseService: DatabaseService) extends IDatabaseService {
  override def execute(sql: String): CompletableFuture[Unit] = databaseService.execute(sql).toJava.toCompletableFuture

  override def executeQuery(sql: String): CompletableFuture[ResultSet] =
    databaseService.executeQuery(sql).toJava.toCompletableFuture

  override def getConnectionMetaData: CompletableFuture[DatabaseMetaData] =
    databaseService.getConnectionMetaData.toJava.toCompletableFuture

  override def closeConnection(): CompletableFuture[Unit] =
    databaseService.closeConnection().toJava.toCompletableFuture
}
