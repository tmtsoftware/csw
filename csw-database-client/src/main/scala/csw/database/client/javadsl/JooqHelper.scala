package csw.database.client.javadsl
import java.util.concurrent.CompletableFuture

import org.jooq.{Queries, Record, ResultQuery}

object JooqHelper {
  def executeBatch(queries: Queries): CompletableFuture[Array[Int]] = AsyncHelper.managedBlock(() ⇒ queries.executeBatch())
  def fetchAsync[R](query: ResultQuery[Record], klass: Class[R]): CompletableFuture[java.util.List[R]] =
    query.fetchAsync().toCompletableFuture.thenApply(x ⇒ x.into(klass))
}
