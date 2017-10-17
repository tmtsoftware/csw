package csw.trombone.hcd.actors

abstract class TickHandler {
  def onTick(): Unit
  def onEnd(): Unit
}
