package csw.database.client.internal

import java.util
import java.util.concurrent.CompletableFuture
import java.util.function

import csw.database.api.javadasl.{DBRow, IDatabaseService}
import csw.database.api.scaladsl.DatabaseService
import slick.jdbc.GetResult

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JDatabaseServiceImpl(databaseService: DatabaseService)(implicit ec: ExecutionContext) extends IDatabaseService {
  override def executeQuery[T](query: String, mapper: function.Function[DBRow, T]): CompletableFuture[util.List[T]] =
    databaseService
      .executeQuery(query)(GetResult(pr => mapper(new DBRow(pr))))
      .map(_.asJava)
      .toJava
      .toCompletableFuture

  override def execute(query: String): CompletableFuture[Int] =
    databaseService.execute(query).toJava.toCompletableFuture

  override def execute(queries: util.List[String]): CompletableFuture[Unit] =
    databaseService.execute(queries.asScala.toList).toJava.toCompletableFuture

}
