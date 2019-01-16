package csw.database.scaladsl
import org.jooq.{Queries, Query, Record, ResultQuery}

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
 * A scala extension, extending few of the Jooq operations.`JOOQ` is a library that provides the mechanism to communicate with
 * Database server. To know in detail about Jooq please refer [[https://www.jooq.org/learn/]].
 */
object JooqExtentions {

  /**
   * An extension on Jooq's ResultQuery#fetchAsync()
   *
   * @param resultQuery Jooq's `ResultQuery` type
   */
  implicit class RichResultQuery(val resultQuery: ResultQuery[Record]) extends AnyVal {

    /**
     * Fetches the result in a Future. It is a wrapper on Jooq's ResultQuery#fetchAsync().
     *
     * @param classTag the class of type `'R'` used to cast the result data
     * @param ec ExecutionContext on which the async fetch call gets scheduled
     * @tparam R the type to which result data gets casted
     * @return a Future that completes with a list of data `'R'`
     */
    def fetchAsyncScala[R](implicit classTag: ClassTag[R], ec: ExecutionContext): Future[List[R]] = {
      val klass = classTag.runtimeClass.asInstanceOf[Class[R]]
      resultQuery
        .fetchAsync()
        .toScala
        .map(_.asScala.map(_.into(klass)).toList)
    }
  }

  /**
   * An extension on Jooq's Query#executeAsync()
   *
   * @param query Jooq's `Query` type
   */
  implicit class RichQuery(val query: Query) extends AnyVal {

    /**
     * Execute the query in a Future. It is a wrapper on Jooq's Query#executeAsunc().
     *
     * @return A Future that completes with an integer. The integer, if it is greater than or equal to zero,
     *         indicates that the command was processed successfully and is an update count giving the number of rows in the
     *         database that were affected by the command's execution.
     */
    def executeAsyncScala(): Future[Integer] = query.executeAsync().toScala
  }

  /**
   * An extension on Jooq's Queries#executeBatch()
   *
   * @param queries Jooq's `Queries` type
   */
  implicit class RichQueries(val queries: Queries) extends AnyVal {

    /**
     * Executes the batch of queries asynchronously. It wraps the Jooq's DSLContext#executeBatch() in a Future
     * and sends the entire batch of queries to database server for execution.
     *
     * @param ec ExecutionContext on which the async batch call gets scheduled
     * @return A future that completes with an array of integers. The integer, if it is greater than or equal to zero,
     *         indicates that the command was processed successfully and is an update count giving the number of rows in the
     *         database that were affected by the command's execution.
     *
     */
    def executeBatchAsync()(implicit ec: ExecutionContext): Future[List[Int]] = {
      Future {
        concurrent.blocking {
          queries.executeBatch().toList
        }
      }
    }
  }

}
