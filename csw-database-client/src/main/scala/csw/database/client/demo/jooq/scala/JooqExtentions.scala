package csw.database.client.demo.jooq.scala
import org.jooq.{Queries, Query, Record, ResultQuery}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object JooqExtentions {
  implicit class RichResultQuery[T <: Record](val resultQuery: ResultQuery[T]) extends AnyVal {
    def fetchAsyncScala[R](implicit classTag: ClassTag[R], ec: ExecutionContext): Future[List[R]] = {
      val klass = classTag.runtimeClass.asInstanceOf[Class[R]]
      resultQuery
        .fetchAsync()
        .toScala
        .map(_.asScala.map(_.into(klass)).toList)
    }
  }

  implicit class RichQuery(val query: Query) extends AnyVal {
    def executeAsyncScala(): Future[Integer] = query.executeAsync().toScala
  }

  implicit class RichQueries(val queries: Queries) extends AnyVal {
    def executeBatchAsync()(implicit ec: ExecutionContext): Future[List[Int]] = {
      concurrent.blocking(
        Future { queries.executeBatch().toList }
      )
    }
  }
}
