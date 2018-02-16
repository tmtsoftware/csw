package tmt.production

import csw.apps.containercmd.ContainerCmd

object SequencerApp extends App {

  ContainerCmd.start("Sequencer-Cmd-App", args)

}
