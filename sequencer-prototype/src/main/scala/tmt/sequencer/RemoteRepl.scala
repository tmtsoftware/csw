package tmt.sequencer

import ammonite.sshd._
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator

object RemoteRepl {
  private def sshServerConfig = SshServerConfig(
    address = "localhost", // or "0.0.0.0" for public-facing shells
    port = 22222, // Any available port
    passwordAuthenticator = Some(AcceptAllPasswordAuthenticator.INSTANCE) // or publicKeyAuthenticator
  )

// for running repl in IDE add below line to initialCommands
// repl.frontEnd() = ammonite.repl.FrontEnd.JLineUnix

  private def initialCommands: String =
    """
      |import tmt.sequencer.Dsl._
      |def setFlags() = repl.compiler.settings.Ydelambdafy.value = "inline"
    """.stripMargin

  def server = new SshdRepl(sshServerConfig, initialCommands)
}
