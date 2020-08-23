package csw.command.client.handlers

import csw.command.api.messages.CommandServiceRequest

object TestHelper {
  implicit class Narrower(x: CommandServiceRequest) {
    def narrow: CommandServiceRequest = x
  }

}
