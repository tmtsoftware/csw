package csw.database.api.scaladsl

import csw.database.api.scaladsl.Aliases.{SelectQuery, UpdateStatement}

import scala.concurrent.Future

trait DatabaseService {
  def query[T](sql: SelectQuery[T]): Future[Seq[T]]
  def update(sqlu: UpdateStatement): Future[Int]
  def updateAll(sqlu: List[UpdateStatement]): Future[Unit]
}
