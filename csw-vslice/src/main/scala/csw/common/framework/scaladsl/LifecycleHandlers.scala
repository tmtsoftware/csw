package csw.common.framework.scaladsl

import csw.common.framework.models.DomainMsg

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class LifecycleHandlers[Msg <: DomainMsg: ClassTag] {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}
