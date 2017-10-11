package csw.trombone.assembly.actors

import csw.messages.params.generics.KeyType.ChoiceKey
import csw.messages.params.generics.{KeyType, _}
import csw.messages.params.models.Choice
import csw.trombone.assembly.commands.AssemblyState

object TromboneState {

  // Keys for state telemetry item
  val cmdUninitialized = Choice("uninitialized")
  val cmdReady         = Choice("ready")
  val cmdBusy          = Choice("busy")
  val cmdContinuous    = Choice("continuous")
  val cmdError         = Choice("error")
  val cmdKey           = ChoiceKey.make("cmd", cmdUninitialized, cmdReady, cmdBusy, cmdContinuous, cmdError)
  val cmdDefault       = cmdItem(cmdUninitialized)

  def cmdItem(ch: Choice): Parameter[Choice] = cmdKey.set(ch)

  val moveUnindexed = Choice("unindexed")
  val moveIndexing  = Choice("indexing")
  val moveIndexed   = Choice("indexed")
  val moveMoving    = Choice("moving")
  val moveKey       = ChoiceKey.make("move", moveUnindexed, moveIndexing, moveIndexed, moveMoving)
  val moveDefault   = moveItem(moveUnindexed)

  def moveItem(ch: Choice): Parameter[Choice] = moveKey.set(ch)

  def sodiumKey          = KeyType.BooleanKey.make("sodiumLayer")
  val sodiumLayerDefault = sodiumItem(false)

  def sodiumItem(flag: Boolean): Parameter[Boolean] = sodiumKey.set(flag)

  def nssKey     = KeyType.BooleanKey.make("nss")
  val nssDefault = nssItem(false)

  def nssItem(flag: Boolean): Parameter[Boolean] = nssKey.set(flag)

  val defaultTromboneState = TromboneState(cmdDefault, moveDefault, sodiumLayerDefault, nssDefault)

  case class TromboneState(cmd: Parameter[Choice],
                           move: Parameter[Choice],
                           sodiumLayer: Parameter[Boolean],
                           nss: Parameter[Boolean])
      extends AssemblyState {

    val cmdChoice: Choice         = cmd.head
    val moveChoice: Choice        = move.head
    val sodiumLayerValue: Boolean = sodiumLayer.head
    val nssValue: Boolean         = nss.head

  }
}
