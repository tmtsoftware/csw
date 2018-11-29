package csw.database.api.javadasl

import java.util.concurrent.CompletableFuture

trait IDatabaseService {

  def executeQuery[T](
      query: String,
      mapper: java.util.function.Function[DBRow, T]
  ): CompletableFuture[java.util.List[T]]

  def execute(query: String): CompletableFuture[Int]

  def execute(queries: java.util.List[String]): CompletableFuture[Unit]

}
