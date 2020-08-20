package csw.command.client.handlers

import csw.command.api.messages.CommandServiceHttpMessage

object TestHelper {
  implicit class Narrower(x: CommandServiceHttpMessage) {
    def narrow: CommandServiceHttpMessage = x
  }

}
