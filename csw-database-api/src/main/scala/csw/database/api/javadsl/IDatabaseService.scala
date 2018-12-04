package csw.database.api.javadsl
import java.util.concurrent.CompletableFuture
import java.util.function.{Consumer, Function}

import csw.database.api.models.{DBRow, SqlParamStore}
import slick.jdbc.PositionedParameters

trait IDatabaseService {
  def query[T](
      sql: String,
      paramBinder: Consumer[PositionedParameters],
      resultMapper: Function[DBRow, T]
  ): CompletableFuture[java.util.List[T]]

  def query[T](sql: String, resultMapper: Function[DBRow, T]): CompletableFuture[java.util.List[T]]

  def update(sql: String, paramBinder: Consumer[PositionedParameters]): CompletableFuture[Integer]
  def update(sql: String): CompletableFuture[Integer]

  def updateAll(sqlParamStore: SqlParamStore): CompletableFuture[Unit]
  def updateAll(sqls: java.util.List[String]): CompletableFuture[Unit]
}
