package csw.services

import caseapp.{CommandName, HelpMessage, ExtraName => Short}

sealed trait Command

object Command {
  @CommandName("start")
  @HelpMessage("start csw services")
  final case class Start(
      @HelpMessage("start all services")
      @Short("a")
      all: Boolean = false,
      @HelpMessage("start config server")
      @Short("c")
      config: Boolean = false,
      @HelpMessage("start event server")
      @Short("e")
      event: Boolean = false,
      @HelpMessage("start alarm server")
      @Short("r")
      alarm: Boolean = false,
      @HelpMessage("start alarm server")
      @Short("d")
      database: Boolean = false,
      @HelpMessage("start auth/aas service")
      @Short("k")
      auth: Boolean = false,
      @HelpMessage("name of the interface")
      @Short("i")
      interfaceName: Option[String]
  ) extends Command

}
