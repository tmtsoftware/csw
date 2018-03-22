package csw.services.config.server.svn

sealed abstract class PathStatus

/**
 * Indicates whether a file path is present or not o config server. If present whether it is
 * stored as Annex or Normal
 */
object PathStatus {
  sealed abstract class Present extends PathStatus
  case object NormalSize        extends Present
  case object Annex             extends Present

  case object Missing extends PathStatus
}
