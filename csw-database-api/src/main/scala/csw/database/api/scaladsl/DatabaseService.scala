package csw.database.api.scaladsl

import csw.database.api.scaladsl.Aliases.{Select, Update}

import scala.concurrent.Future

trait DatabaseService {
  def select[T](sql: Select[T]): Future[Seq[T]]
  def update(sqlu: Update): Future[Int]
  def updateAll(sqlu: List[Update]): Future[Unit]
}
