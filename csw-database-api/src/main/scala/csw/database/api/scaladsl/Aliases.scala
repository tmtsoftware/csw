package csw.database.api.scaladsl

import slick.dbio.{DBIOAction, Effect, NoStream}

object Aliases {
  type SelectQuery[T]  = DBIOAction[Seq[T], NoStream, Effect]
  type UpdateStatement = DBIOAction[Int, NoStream, Effect]
}
