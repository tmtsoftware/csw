package csw.common.framework.internal.container

sealed trait ContainerMode
object ContainerMode {
  case object Idle    extends ContainerMode
  case object Running extends ContainerMode
}
