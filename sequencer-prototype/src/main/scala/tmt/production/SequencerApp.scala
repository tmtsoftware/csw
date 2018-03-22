package tmt.production

import csw.framework.deploy.containercmd.ContainerCmd

object SequencerApp extends App {

  ContainerCmd.start("Sequencer-Cmd-App", args)

}
