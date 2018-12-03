package csw.database.api.models
import java.util.function.Consumer

import slick.jdbc.PositionedParameters

class SqlParamStore(private[database] val sqlToParamBinder: Map[String, Consumer[PositionedParameters]]) {
  def add(sql: String, paramBinder: Consumer[PositionedParameters]): SqlParamStore =
    sqlToParamBinder.updated(sql, paramBinder)

  def remove(sql: String): SqlParamStore = sqlToParamBinder - sql
}

object SqlParamStore {
  implicit private def apply(store: Map[String, Consumer[PositionedParameters]]): SqlParamStore = new SqlParamStore(store)
}
