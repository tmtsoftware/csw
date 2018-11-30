package csw.database.api.scaladsl

import slick.dbio.{DBIOAction, Effect, NoStream}

object Aliases {
  type Select[T] = DBIOAction[Seq[T], NoStream, Effect]
  type Update    = DBIOAction[Int, NoStream, Effect]
}
