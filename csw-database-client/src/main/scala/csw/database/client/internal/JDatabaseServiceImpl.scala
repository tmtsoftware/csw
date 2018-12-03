package csw.database.client.internal
import java.util
import java.util.concurrent.CompletableFuture
import java.util.function
import java.util.function.Consumer

import csw.database.api.javadsl.IDatabaseService
import csw.database.api.models.{DBRow, SqlParamStore}
import csw.database.api.scaladsl.Aliases.Update
import csw.database.api.scaladsl.DatabaseService
import slick.jdbc.{GetResult, PositionedParameters, SQLActionBuilder, SetParameter}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JDatabaseServiceImpl(databaseService: DatabaseService, ec: ExecutionContext) extends IDatabaseService {
  override def select[T](
      sql: String,
      paramBinder: Consumer[PositionedParameters],
      resultMapper: function.Function[DBRow, T]
  ): CompletableFuture[util.List[T]] =
    selectInternal(sql, toSlickParam(paramBinder), resultMapper)

  override def select[T](sql: String, resultMapper: function.Function[DBRow, T]): CompletableFuture[util.List[T]] =
    selectInternal(sql, SetParameter.SetUnit, resultMapper)

  private def selectInternal[T](
      sql: String,
      setParameter: SetParameter[Unit],
      resultMapper: function.Function[DBRow, T]
  ): CompletableFuture[util.List[T]] = {
    val action = SQLActionBuilder(sql, setParameter).as(toSlickResult(resultMapper))
    databaseService.select(action).map(seqAsJavaList)(ec).toJava.toCompletableFuture
  }

  override def update(sql: String, paramBinder: Consumer[PositionedParameters]): CompletableFuture[Integer] =
    updateInternal(sql, toSlickParam(paramBinder))

  override def update(sql: String): CompletableFuture[Integer] = updateInternal(sql, SetParameter.SetUnit)

  private def updateInternal(sql: String, setParameter: SetParameter[Unit]): CompletableFuture[Integer] = {
    val action = SQLActionBuilder(sql, setParameter).asUpdate
    databaseService.update(action).map(x => x.asInstanceOf[Integer])(ec).toJava.toCompletableFuture
  }

  override def updateAll(sqlParamStore: SqlParamStore): CompletableFuture[Unit] = {
    val actions: List[Update] = sqlParamStore.sqlToParamBinder.map {
      case (sql, paramBinder) ⇒ SQLActionBuilder(sql, toSlickParam(paramBinder)).asUpdate
    }.toList

    databaseService.updateAll(actions).toJava.toCompletableFuture
  }

  override def updateAll(sqls: util.List[String]): CompletableFuture[Unit] = {
    val update: List[Update] = sqls.asScala.toList.map(sql ⇒ SQLActionBuilder(sql, SetParameter.SetUnit).asUpdate)
    databaseService.updateAll(update).toJava.toCompletableFuture
  }

  private def toSlickParam(paramBinder: Consumer[PositionedParameters]): SetParameter[Unit] =
    (_: Unit, pp: PositionedParameters) ⇒ paramBinder.accept(pp)
  private def toSlickResult[T](resultMapper: function.Function[DBRow, T]): GetResult[T] =
    GetResult(pr ⇒ resultMapper(new DBRow(pr)))
}
