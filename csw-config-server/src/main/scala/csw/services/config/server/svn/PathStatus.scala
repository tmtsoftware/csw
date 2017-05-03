package csw.services.config.server.svn

sealed abstract class PathStatus

object PathStatus {
  sealed abstract class Present extends PathStatus
  case object NormalSize        extends Present
  case object Annex             extends Present

  case object Missing extends PathStatus
}
