package csw.common.framework.scaladsl

import scala.concurrent.Future
import scala.reflect.ClassTag

abstract class LifecycleHandlers[Msg: ClassTag] {
  var isOnline: Boolean = false

  def initialize(): Future[Unit]
  def onRun(): Unit
  def onDomainMsg(msg: Msg): Unit

  def onShutdown(): Unit
  def onRestart(): Unit
  def onGoOffline(): Unit
  def onGoOnline(): Unit
}
