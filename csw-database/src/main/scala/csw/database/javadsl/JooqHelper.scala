package csw.database.javadsl
import java.util.concurrent.CompletableFuture

import org.jooq.{Queries, Record, ResultQuery}

/**
 * A java helper wrapping few of the Jooq operations. `JOOQ` is a library that provides the mechanism to communicate with
 * Database server. To know in detail about Jooq please refer [[https://www.jooq.org/learn/]].
 */
object JooqHelper {

  /**
   * Executes the batch of queries asynchronously. It wraps the Jooq's DSLContext#executeBatch() in a CompletableFuture
   * and sends the entire batch of queries to database server for execution.
   *
   * @param queries A Jooq type that represents a set of queries to execute in batch
   * @return A completable future that completes with an array of integers. The integer, if it is greater than or equal to zero,
   *         indicates that the command was processed successfully and is an update count giving the number of rows in the
   *         database that were affected by the command's execution.
   */
  def executeBatch(queries: Queries): CompletableFuture[Array[Int]] = AsyncHelper.managedBlock(() ⇒ queries.executeBatch())

  /**
   * Fetches the result in a CompletableFuture. It is a wrapper on Jooq's ResultQuery#fetchAsync().
   *
   * @param query the select query to fetch the data from database
   * @param klass the class of type `'R'` used to cast the result data
   * @tparam R the type to which result data gets casted
   * @return a CompletableFuture that completes with a list of data `'R'`
   */
  def fetchAsync[R](query: ResultQuery[Record], klass: Class[R]): CompletableFuture[java.util.List[R]] =
    query.fetchAsync().toCompletableFuture.thenApply(x ⇒ x.into(klass))
}
